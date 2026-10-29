package com.muzikApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.muzikApp.dto.request.ChangePasswordRequest;
import com.muzikApp.dto.request.UpdateProfileRequest;
import com.muzikApp.dto.response.AppUserResponse;
import com.muzikApp.dto.response.MessageResponse;
import com.muzikApp.service.AppUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AppUserService userService;

    public UserController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public AppUserResponse me() {
        return userService.me();
    }

    @PutMapping
    public AppUserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @PutMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return new MessageResponse("Password changed. Please log in again.");
    }
}
