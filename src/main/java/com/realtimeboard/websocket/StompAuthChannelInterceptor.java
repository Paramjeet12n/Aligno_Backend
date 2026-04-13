package com.realtimeboard.websocket;

import com.realtimeboard.exception.ApiException;
import com.realtimeboard.security.AppUserDetails;
import com.realtimeboard.security.AppUserDetailsService;
import com.realtimeboard.security.JwtService;
import com.realtimeboard.service.BoardAccessService;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;
  private final AppUserDetailsService userDetailsService;
  private final BoardAccessService boardAccessService;

  public StompAuthChannelInterceptor(
      JwtService jwtService,
      AppUserDetailsService userDetailsService,
      BoardAccessService boardAccessService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.boardAccessService = boardAccessService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String bearer = firstNative(accessor, "Authorization");
      if (bearer == null || !bearer.startsWith("Bearer ")) {
        throw new MessageDeliveryException(message, "Missing or invalid Authorization header");
      }
      String token = bearer.substring("Bearer ".length()).trim();
      try {
        Long userId = jwtService.getUserId(token);
        AppUserDetails user = userDetailsService.loadById(userId);
        accessor.setUser(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
      } catch (RuntimeException e) {
        throw new MessageDeliveryException(message, "Invalid token");
      }
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      String dest = accessor.getDestination();
      if (dest != null && dest.startsWith("/topic/board/")) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !auth.isAuthenticated()) {
          throw new MessageDeliveryException(message, "Unauthorized");
        }
        if (!(auth.getPrincipal() instanceof AppUserDetails principal)) {
          throw new MessageDeliveryException(message, "Unauthorized");
        }
        String suffix = dest.substring("/topic/board/".length());
        String boardIdPart = suffix;
        int slash = suffix.indexOf('/');
        if (slash != -1) {
          boardIdPart = suffix.substring(0, slash);
        }
        long boardId;
        try {
          boardId = Long.parseLong(boardIdPart);
        } catch (NumberFormatException e) {
          throw new MessageDeliveryException(message, "Invalid board topic");
        }
        try {
          boardAccessService.requireMemberOrOwner(boardId, principal.getId());
        } catch (ApiException e) {
          throw new MessageDeliveryException(message, "Forbidden: not a member of this board");
        }
      }
    }

    return message;
  }

  private static String firstNative(StompHeaderAccessor accessor, String name) {
    List<String> headers = accessor.getNativeHeader(name);
    if (headers == null || headers.isEmpty()) {
      return null;
    }
    return headers.get(0);
  }
}
