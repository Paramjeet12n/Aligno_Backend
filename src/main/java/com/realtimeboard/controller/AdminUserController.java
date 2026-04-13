package com.realtimeboard.controller;

import com.realtimeboard.dto.admin.AdminUserDtos.AdminCreateUserRequest;
import com.realtimeboard.dto.auth.AuthDtos.UserResponse;
import com.realtimeboard.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @PostMapping
  public UserResponse create(@Valid @RequestBody AdminCreateUserRequest req) {
    return adminUserService.createUser(req);
  }
}
