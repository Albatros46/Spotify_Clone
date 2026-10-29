package com.muzikApp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public sign-up. There is deliberately NO role field: otherwise anyone could register as ADMIN.
 * Every new account is a USER.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    @NotBlank(message = "Name is required.")
    @Size(min = 3, max = 50,message = "Name must be between 3 and 50 characters!")
    private String name;
    @NotBlank(message = "Email is required.")
    @Email(message = "Email should be valid.")
    private String email;
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String password;
}
