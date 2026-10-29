package com.muzikApp.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.muzikApp.config.AppProperties;
import com.muzikApp.dto.request.PlayListRequest;
import com.muzikApp.dto.response.PaginatedResponse;
import com.muzikApp.dto.response.PlayListResponse;
import com.muzikApp.dto.response.PlaylistWithSongResponse;
import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Playlist;
import com.muzikApp.entities.PlaylistSong;
import com.muzikApp.entities.Song;
import com.muzikApp.exception.ConflictException;
import com.muzikApp.exception.ResourceNotFoundException;
import com.muzikApp.repository.PlaylistRepository;
import com.muzikApp.repository.PlaylistSongRepository;
import com.muzikApp.repository.SongRepository;

/**
 * Only the owner may change a playlist. A private playlist of someone else answers 404, not 403:
 * otherwise a client could find out which playlist ids exist.
 */
@Service
public class PlaylistService {

    private final PlaylistRepository playlists;
    private final PlaylistSongRepository entries;
    private final SongRepository songs;
    private final CurrentUserService currentUser;
    private final String baseUrl;

    public PlaylistService(PlaylistRepository playlists, PlaylistSongRepository entries, SongRepository songs,
            CurrentUserService currentUser, AppProperties properties) {
        this.playlists = playlists;
        this.entries = entries;
        this.songs = songs;
        this.currentUser = currentUser;
        this.baseUrl = properties.baseUrl();
    }

    @Transactional
    public PlayListResponse create(PlayListRequest request) {
        Playlist playlist = new Playlist();
        playlist.setAppUser(currentUser.get());
        apply(playlist, request);
        return PlayListResponse.fromEntity(playlists.save(playlist), baseUrl);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PlayListResponse> mine(Pageable pageable) {
        return PaginatedResponse.from(playlists.findByAppUser(currentUser.get(), pageable)
                .map(p -> PlayListResponse.fromEntity(p, baseUrl)));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PlayListResponse> publicPlaylists(Pageable pageable) {
        return PaginatedResponse.from(playlists.findByIsPublicTrue(pageable)
                .map(p -> PlayListResponse.fromEntity(p, baseUrl)));
    }

    @Transactional(readOnly = true)
    public PlaylistWithSongResponse get(Long id) {
        Playlist playlist = visible(id);
        return PlaylistWithSongResponse.fromEntity(playlist, entries.findByPlaylistOrderByPositionAsc(playlist),
                baseUrl);
    }

    @Transactional
    public PlayListResponse update(Long id, PlayListRequest request) {
        Playlist playlist = owned(id);
        apply(playlist, request);
        return PlayListResponse.fromEntity(playlists.saveAndFlush(playlist), baseUrl);
    }

    @Transactional
    public void delete(Long id) {
        Playlist playlist = owned(id);
        entries.deleteByPlaylist(playlist);
        playlists.delete(playlist);
    }

    @Transactional
    public PlaylistWithSongResponse addSong(Long playlistId, Long songId) {
        Playlist playlist = owned(playlistId);
        Song song = songs.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song " + songId + " not found"));
        if (entries.existsByPlaylistAndSong(playlist, song)) {
            throw new ConflictException("Song " + songId + " is already in this playlist");
        }
        PlaylistSong entry = new PlaylistSong();
        entry.setPlaylist(playlist);
        entry.setSong(song);
        entry.setPosition(entries.maxPosition(playlist) + 1);
        entries.saveAndFlush(entry);
        return get(playlistId);
    }

    @Transactional
    public PlaylistWithSongResponse removeSong(Long playlistId, Long songId) {
        Playlist playlist = owned(playlistId);
        Song song = songs.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song " + songId + " not found"));
        PlaylistSong entry = entries.findByPlaylistAndSong(playlist, song)
                .orElseThrow(() -> new ResourceNotFoundException("Song " + songId + " is not in this playlist"));
        entries.delete(entry);
        entries.shiftUpAfter(playlist, entry.getPosition());
        return get(playlistId);
    }

    private void apply(Playlist playlist, PlayListRequest request) {
        playlist.setName(request.getName().trim());
        playlist.setDescription(request.getDescription());
        playlist.setPublic(Boolean.TRUE.equals(request.getIsPublic())); // default: private
    }

    /** Public playlists and the caller's own ones. */
    private Playlist visible(Long id) {
        Playlist playlist = find(id);
        if (!playlist.isPublic() && !isOwner(playlist)) {
            throw notFound(id);
        }
        return playlist;
    }

    /** Only the caller's own playlists; for anything else the playlist "does not exist". */
    private Playlist owned(Long id) {
        Playlist playlist = find(id);
        if (!isOwner(playlist)) {
            throw notFound(id);
        }
        return playlist;
    }

    private boolean isOwner(Playlist playlist) {
        AppUser me = currentUser.get();
        return playlist.getAppUser().getId().equals(me.getId());
    }

    private Playlist find(Long id) {
        return playlists.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Playlist " + id + " not found");
    }
}
