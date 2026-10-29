package com.muzikApp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only what a user may change about themselves (no role, no token). */
public record UpdateProfileRequest(
        @NotBlank(message = "Name is required.")
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters!")
        String name,
        @NotBlank(message = "Email is required.")
        @Email(message = "Email should be valid.")
        String email) {
}
