package com.realtimeboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AppUserDetailsService userDetailsService;

  public JwtAuthFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Do not parse JWT on login/register so a stale {@code Authorization} header cannot affect those
   * requests or pollute the security context.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return false;
    }
    String path = requestPathWithoutContext(request);
    return "/auth/login".equals(path) || "/auth/register".equals(path);
  }

  private static String requestPathWithoutContext(HttpServletRequest request) {
    String uri = request.getRequestURI();
    int q = uri.indexOf('?');
    if (q >= 0) {
      uri = uri.substring(0, q);
    }
    String ctx = request.getContextPath();
    if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
      uri = uri.substring(ctx.length());
    }
    return uri.isEmpty() ? "/" : uri;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String token = auth.substring("Bearer ".length()).trim();
      Long userId = jwtService.getUserId(token);
      AppUserDetails userDetails = userDetailsService.loadById(userId);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (RuntimeException e) {
      // invalid token -> treat as unauthenticated
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}

