package com.muzikApp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.muzikApp.dto.request.LoginUserRequest;
import com.muzikApp.dto.request.RefreshTokenRequest;
import com.muzikApp.dto.request.RegisterUserRequest;
import com.muzikApp.dto.response.AppUserResponse;
import com.muzikApp.dto.response.MessageResponse;
import com.muzikApp.service.AuthService;
import com.muzikApp.service.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUser;

    public AuthController(AuthService authService, CurrentUserService currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AppUserResponse login(@Valid @RequestBody LoginUserRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AppUserResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public MessageResponse logout() {
        authService.logout(currentUser.get());
        return new MessageResponse("Logged out");
    }
}
