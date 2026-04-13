package com.realtimeboard.service;

import com.realtimeboard.dto.admin.AdminUserDtos.AdminCreateUserRequest;
import com.realtimeboard.dto.auth.AuthDtos.UserResponse;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.UserRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse createUser(AdminCreateUserRequest req) {
    String email = req.email().trim().toLowerCase();
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
    }

    User u = new User();
    u.setName(req.name().trim());
    u.setEmail(email);
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setCreatedAt(Instant.now());
    u.setAppRole(req.role());
    u = userRepository.save(u);

    return UserResponse.builder()
        .id(u.getId())
        .name(u.getName())
        .email(u.getEmail())
        .role(u.getAppRole().name())
        .build();
  }
}
