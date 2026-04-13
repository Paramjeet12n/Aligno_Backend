package com.realtimeboard.service;

import com.realtimeboard.dto.auth.AuthDtos.AuthResponse;
import com.realtimeboard.dto.auth.AuthDtos.LoginRequest;
import com.realtimeboard.dto.auth.AuthDtos.RegisterRequest;
import com.realtimeboard.dto.auth.AuthDtos.UserResponse;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.AppUserRole;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.UserRepository;
import com.realtimeboard.security.JwtService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (userRepository.existsByEmailIgnoreCase(req.email())) {
      throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
    }

    User u = new User();
    u.setName(req.name().trim());
    u.setEmail(req.email().trim().toLowerCase());
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setCreatedAt(Instant.now());
    u.setAppRole(AppUserRole.USER);
    u = userRepository.save(u);

    String token = jwtService.generateToken(u.getId(), u.getEmail());
    return AuthResponse.builder().token(token).user(toUserResponse(u)).build();
  }

  @Transactional(readOnly = true)
  public UserResponse me(Long userId) {
    User u =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));
    return toUserResponse(u);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    String email = req.email().trim().toLowerCase();
    User u =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));

    boolean passwordOk;
    try {
      passwordOk = passwordEncoder.matches(req.password(), u.getPasswordHash());
    } catch (IllegalArgumentException ex) {
      // Stored hash is not a valid BCrypt string (legacy/plaintext row) — treat like bad login.
      passwordOk = false;
    }
    if (!passwordOk) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    String token = jwtService.generateToken(u.getId(), u.getEmail());
    return AuthResponse.builder().token(token).user(toUserResponse(u)).build();
  }

  private static UserResponse toUserResponse(User u) {
    return UserResponse.builder()
        .id(u.getId())
        .name(u.getName())
        .email(u.getEmail())
        .role(u.getAppRole() == null ? AppUserRole.USER.name() : u.getAppRole().name())
        .build();
  }
}

