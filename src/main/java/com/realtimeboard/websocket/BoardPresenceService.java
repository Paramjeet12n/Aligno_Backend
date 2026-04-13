package com.realtimeboard.websocket;

import com.realtimeboard.security.AppUserDetails;
import com.realtimeboard.websocket.dto.PresenceSnapshot;
import com.realtimeboard.websocket.dto.PresenceSnapshot.PresenceUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Service
public class BoardPresenceService {

  private final SimpMessagingTemplate messagingTemplate;

  /** boardId -> (sessionId -> user) */
  private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PresenceUser>> byBoard =
      new ConcurrentHashMap<>();

  /** sessionId -> boardId(s) (usually 1) */
  private final ConcurrentHashMap<String, ConcurrentHashMap<Long, Boolean>> boardsBySession =
      new ConcurrentHashMap<>();

  public BoardPresenceService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @EventListener
  public void onSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String dest = accessor.getDestination();
    if (dest == null) return;
    if (!dest.startsWith("/topic/board/") || !dest.endsWith("/presence")) return;

    Long boardId = parseBoardId(dest);
    if (boardId == null) return;

    String sessionId = accessor.getSessionId();
    if (sessionId == null) return;

    if (!(accessor.getUser() instanceof org.springframework.security.core.Authentication auth)) return;
    if (!(auth.getPrincipal() instanceof AppUserDetails principal)) return;

    PresenceUser user =
        PresenceUser.builder().userId(principal.getId()).name(principal.getName()).build();

    byBoard.computeIfAbsent(boardId, _k -> new ConcurrentHashMap<>()).put(sessionId, user);
    boardsBySession.computeIfAbsent(sessionId, _k -> new ConcurrentHashMap<>()).put(boardId, true);

    publish(boardId);
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    if (sessionId == null) return;

    Map<Long, Boolean> boards = boardsBySession.remove(sessionId);
    if (boards == null) return;

    for (Long boardId : boards.keySet()) {
      ConcurrentHashMap<String, PresenceUser> sessions = byBoard.get(boardId);
      if (sessions == null) continue;
      sessions.remove(sessionId);
      if (sessions.isEmpty()) {
        byBoard.remove(boardId, sessions);
      }
      publish(boardId);
    }
  }

  private void publish(long boardId) {
    ConcurrentHashMap<String, PresenceUser> sessions = byBoard.get(boardId);
    ArrayList<PresenceUser> users = new ArrayList<>();
    if (sessions != null) {
      users.addAll(sessions.values());
    }
    // Remove duplicates (same user multiple tabs) by userId, keep first
    Map<Long, PresenceUser> dedup = new java.util.LinkedHashMap<>();
    for (PresenceUser u : users) {
      dedup.putIfAbsent(u.getUserId(), u);
    }
    ArrayList<PresenceUser> out = new ArrayList<>(dedup.values());
    out.sort(Comparator.comparing(PresenceUser::getName, String.CASE_INSENSITIVE_ORDER));

    messagingTemplate.convertAndSend(
        "/topic/board/" + boardId + "/presence",
        PresenceSnapshot.builder().boardId(boardId).timestamp(Instant.now()).users(out).build());
  }

  private static Long parseBoardId(String dest) {
    // /topic/board/{id}/presence
    String suffix = dest.substring("/topic/board/".length());
    int slash = suffix.indexOf('/');
    if (slash == -1) return null;
    String idPart = suffix.substring(0, slash);
    try {
      return Long.parseLong(idPart);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}

