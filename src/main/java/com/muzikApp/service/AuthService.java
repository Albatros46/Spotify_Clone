package com.muzikApp.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muzikApp.dto.request.LoginUserRequest;
import com.muzikApp.dto.request.RegisterUserRequest;
import com.muzikApp.dto.response.AppUserResponse;
import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Role;
import com.muzikApp.exception.ConflictException;
import com.muzikApp.exception.InvalidTokenException;
import com.muzikApp.repository.AppUserRepository;
import com.muzikApp.security.JwtService;
import com.muzikApp.security.TokenHasher;

@Service
public class AuthService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AppUserResponse register(RegisterUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        AppUser user = new AppUser();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // never taken from the request
        return issueTokens(repository.save(user));
    }

    @Transactional
    public AppUserResponse login(LoginUserRequest request) {
        // throws BadCredentialsException -> 401 "Invalid email or password" (same message for both cases)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword()));
        AppUser user = repository.findByEmailIgnoreCase(request.getEmail().trim()).orElseThrow();
        return issueTokens(user);
    }

    /**
     * Rotation: every refresh returns a NEW refresh token and the old one stops working.
     * A stolen refresh token can therefore be used at most once before the real user notices.
     */
    @Transactional
    public AppUserResponse refresh(String refreshToken) {
        String email = jwtService.emailFromRefreshToken(refreshToken);
        AppUser user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
        if (!TokenHasher.matches(refreshToken, user.getRefreshToken())) {
            throw new InvalidTokenException("Refresh token was already used or revoked");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(AppUser user) {
        user.setRefreshToken(null);
        repository.save(user);
    }

    private AppUserResponse issueTokens(AppUser user) {
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);
        user.setRefreshToken(TokenHasher.sha256(refresh));
        repository.save(user);
        return AppUserResponse.fromEntity(user, access, refresh);
    }
}
