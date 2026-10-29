package com.muzikApp.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.muzikApp.dto.request.PlayListRequest;
import com.muzikApp.dto.response.PaginatedResponse;
import com.muzikApp.dto.response.PlayListResponse;
import com.muzikApp.dto.response.PlaylistWithSongResponse;
import com.muzikApp.service.PlaylistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayListResponse create(@Valid @RequestBody PlayListRequest request) {
        return playlistService.create(request);
    }

    @GetMapping("/me")
    public PaginatedResponse<PlayListResponse> mine(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return playlistService.mine(pageable);
    }

    @GetMapping("/public")
    public PaginatedResponse<PlayListResponse> publicPlaylists(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return playlistService.publicPlaylists(pageable);
    }

    @GetMapping("/{id}")
    public PlaylistWithSongResponse get(@PathVariable Long id) {
        return playlistService.get(id);
    }

    @PutMapping("/{id}")
    public PlayListResponse update(@PathVariable Long id, @Valid @RequestBody PlayListRequest request) {
        return playlistService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        playlistService.delete(id);
    }

    @PostMapping("/{id}/songs/{songId}")
    public PlaylistWithSongResponse addSong(@PathVariable Long id, @PathVariable Long songId) {
        return playlistService.addSong(id, songId);
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public PlaylistWithSongResponse removeSong(@PathVariable Long id, @PathVariable Long songId) {
        return playlistService.removeSong(id, songId);
    }
}
