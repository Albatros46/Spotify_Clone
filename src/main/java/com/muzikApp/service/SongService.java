package com.muzikApp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.muzikApp.config.AppProperties;
import com.muzikApp.dto.request.SongRequest;
import com.muzikApp.dto.response.PaginatedResponse;
import com.muzikApp.dto.response.SongResponse;
import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Playlist;
import com.muzikApp.entities.Song;
import com.muzikApp.exception.ResourceNotFoundException;
import com.muzikApp.repository.PlaylistSongRepository;
import com.muzikApp.repository.SongRepository;

@Service
public class SongService {

    private final SongRepository songs;
    private final PlaylistSongRepository playlistSongs;
    private final FileStorageService storage;
    private final CurrentUserService currentUser;
    private final String baseUrl;

    public SongService(SongRepository songs, PlaylistSongRepository playlistSongs, FileStorageService storage,
            CurrentUserService currentUser, AppProperties properties) {
        this.songs = songs;
        this.playlistSongs = playlistSongs;
        this.storage = storage;
        this.currentUser = currentUser;
        this.baseUrl = properties.baseUrl();
    }

    @Transactional
    public SongResponse upload(SongRequest request, MultipartFile file, MultipartFile image) {
        AppUser owner = currentUser.get();
        // if the image is rejected, the already stored song file is removed again (no orphan files)
        String songUrl = storage.storeSong(file);
        String imageUrl = null;
        try {
            imageUrl = image == null || image.isEmpty() ? null : storage.storeImage(image);
        } catch (RuntimeException e) {
            storage.delete(songUrl);
            throw e;
        }
        Song song = new Song();
        song.setTitle(request.getTitle().trim());
        song.setArtist(request.getArtist().trim());
        song.setAlbum(request.getAlbum().trim());
        song.setSongUrl(songUrl);
        song.setImageUrl(imageUrl);
        song.setAppUser(owner);
        return SongResponse.fromEntity(songs.save(song), baseUrl);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SongResponse> list(String query, Pageable pageable) {
        Page<Song> page = query == null || query.isBlank() ? songs.findAllBy(pageable)
                : songs.search(query.trim(), pageable);
        return PaginatedResponse.from(page.map(song -> SongResponse.fromEntity(song, baseUrl)));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SongResponse> mine(Pageable pageable) {
        return PaginatedResponse.from(songs.findByAppUser(currentUser.get(), pageable)
                .map(song -> SongResponse.fromEntity(song, baseUrl)));
    }

    @Transactional(readOnly = true)
    public SongResponse get(Long id) {
        return SongResponse.fromEntity(find(id), baseUrl);
    }

    /** Owner or admin. The song also disappears from every playlist; the positions there close up. */
    @Transactional
    public void delete(Long id) {
        Song song = find(id);
        if (!CurrentUserService.isOwnerOrAdmin(currentUser.get(), song.getAppUser())) {
            throw new AccessDeniedException("Only the owner or an admin may delete this song");
        }
        for (Playlist playlist : playlistSongs.findPlaylistsContaining(song)) {
            playlistSongs.findByPlaylistAndSong(playlist, song).ifPresent(entry -> {
                playlistSongs.delete(entry);
                playlistSongs.shiftUpAfter(playlist, entry.getPosition());
            });
        }
        songs.delete(song);
        storage.delete(song.getSongUrl());
        storage.delete(song.getImageUrl());
    }

    Song find(Long id) {
        return songs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Song " + id + " not found"));
    }
}
