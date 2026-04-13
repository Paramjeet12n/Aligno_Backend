package com.realtimeboard.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegisterRequest(@NotBlank String name, @Email @NotBlank String email, @NotBlank String password) {}

  /** Email is not {@code @Email}: dev domains and trimming are handled in the controller/service. */
  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  @Value
  @Builder
  public static class UserResponse {
    Long id;
    String name;
    String email;
    String role;
  }

  @Value
  @Builder
  public static class AuthResponse {
    String token;
    UserResponse user;
  }
}

