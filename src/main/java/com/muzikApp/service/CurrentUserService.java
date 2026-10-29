package com.muzikApp.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Role;
import com.muzikApp.exception.InvalidTokenException;
import com.muzikApp.repository.AppUserRepository;

/** The logged-in user, loaded from the database (the token only carries the e-mail). */
@Service
public class CurrentUserService {

    private final AppUserRepository repository;

    public CurrentUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    public AppUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidTokenException("Authentication required");
        }
        return repository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));
    }

    public static boolean isOwnerOrAdmin(AppUser current, AppUser owner) {
        return current.getRole() == Role.ADMIN || current.getId().equals(owner.getId());
    }
}
