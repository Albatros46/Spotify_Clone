package com.muzikApp.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muzikApp.dto.request.ChangePasswordRequest;
import com.muzikApp.dto.request.UpdateProfileRequest;
import com.muzikApp.dto.response.AppUserResponse;
import com.muzikApp.entities.AppUser;
import com.muzikApp.exception.ConflictException;
import com.muzikApp.repository.AppUserRepository;

@Service
public class AppUserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;

    public AppUserService(AppUserRepository repository, PasswordEncoder passwordEncoder,
            CurrentUserService currentUser) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public AppUserResponse me() {
        return AppUserResponse.fromEntity(currentUser.get());
    }

    @Transactional
    public AppUserResponse updateProfile(UpdateProfileRequest request) {
        AppUser user = currentUser.get();
        String email = request.email().trim().toLowerCase();
        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        if (emailChanged && repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        // the tokens carry the old e-mail: after changing it the user has to log in again
        if (emailChanged) {
            user.setRefreshToken(null);
        }
        return AppUserResponse.fromEntity(repository.saveAndFlush(user));
    }

    /** Changing the password also ends every session (the refresh token is revoked). */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser user = currentUser.get();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is wrong");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setRefreshToken(null);
        repository.save(user);
    }
}
