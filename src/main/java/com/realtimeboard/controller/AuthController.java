package com.realtimeboard.controller;

import com.realtimeboard.dto.auth.AuthDtos.AuthResponse;
import com.realtimeboard.dto.auth.AuthDtos.LoginRequest;
import com.realtimeboard.dto.auth.AuthDtos.RegisterRequest;
import com.realtimeboard.dto.auth.AuthDtos.UserResponse;
import com.realtimeboard.security.CurrentUser;
import com.realtimeboard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
    return authService.register(req);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req);
  }

  @GetMapping("/me")
  public UserResponse me() {
    return authService.me(CurrentUser.requireId());
  }
}

