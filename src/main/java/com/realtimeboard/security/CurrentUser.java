package com.realtimeboard.security;

import com.realtimeboard.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
  private CurrentUser() {}

  public static Long id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
    Object p = auth.getPrincipal();
    if (p instanceof AppUserDetails aud) return aud.getId();
    return null;
  }

  /** Use on authenticated routes so services never see a null user id. */
  public static long requireId() {
    Long id = id();
    if (id == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated");
    }
    return id;
  }
}

