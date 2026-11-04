package com.fifotech.taskmanager.service;

import com.fifotech.taskmanager.dto.LoginRequest;
import com.fifotech.taskmanager.dto.LoginResponse;
import com.fifotech.taskmanager.security.JwtService;
import com.fifotech.taskmanager.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Verifies email + password (BCrypt comparison happens inside the AuthenticationManager)
     * and returns a signed JWT. Wrong credentials throw an AuthenticationException,
     * which the GlobalExceptionHandler turns into HTTP 401.
     */
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new LoginResponse(token, principal.getRole().name(), principal.getFullName());
    }
}
