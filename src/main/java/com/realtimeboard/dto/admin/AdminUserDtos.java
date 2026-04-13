package com.realtimeboard.dto.admin;

import com.realtimeboard.model.entity.AppUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminUserDtos {
  private AdminUserDtos() {}

  public record AdminCreateUserRequest(
      @NotBlank String name,
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128) String password,
      @NotNull AppUserRole role) {}
}
