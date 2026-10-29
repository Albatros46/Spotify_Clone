package com.muzikApp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Playlist;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    @EntityGraph(attributePaths = "appUser")
    Page<Playlist> findByAppUser(AppUser appUser, Pageable pageable);

    @EntityGraph(attributePaths = "appUser")
    Page<Playlist> findByIsPublicTrue(Pageable pageable);
}
