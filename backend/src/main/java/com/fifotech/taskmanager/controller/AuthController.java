package com.fifotech.taskmanager.controller;

import com.fifotech.taskmanager.dto.CreateUserRequest;
import com.fifotech.taskmanager.dto.LoginRequest;
import com.fifotech.taskmanager.dto.LoginResponse;
import com.fifotech.taskmanager.dto.UserResponse;
import com.fifotech.taskmanager.service.AuthService;
import com.fifotech.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controllers stay thin: validate input, enforce the role, delegate to a service. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /** Public. Returns { token, role, name } or 401 for wrong credentials. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Admin only: creates a new user (same logic as POST /api/users). */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}
