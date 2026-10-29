package com.muzikApp.dto.response;

import com.muzikApp.entities.Playlist;
import com.muzikApp.entities.PlaylistSong;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistWithSongResponse {
    private Long id;
    private String name;
    private String description;
    /** Boolean, not boolean: keeps the JSON name "isPublic" (see PlayListResponse). */
    private Boolean isPublic;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long appUserId;
    private String appUserName;
    private Integer songCount;
    private List<SongInPlaylistResponse> songs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SongInPlaylistResponse{
        private Long songId;
        private String title;
        private String artist;
        private String album;
        private String songUrl;
        private String imageUrl;
        private Integer position;
        private LocalDateTime addedAt;
    }

    public static PlaylistWithSongResponse fromEntity(Playlist playlist,List<PlaylistSong> playlistSongs,String baseUrl) {
        PlaylistWithSongResponse response  = new PlaylistWithSongResponse();
        response.setId(playlist.getId());
        response.setName(playlist.getName());
        response.setDescription(playlist.getDescription());
        response.setIsPublic(playlist.isPublic());
        response.setImageUrl(withBaseUrl(baseUrl, playlist.getImageUrl()));
        response.setCreatedAt(playlist.getCreatedAt());
        response.setUpdatedAt(playlist.getUpdatedAt());
        response.setAppUserId(playlist.getAppUser().getId());
        response.setAppUserName(playlist.getAppUser().getName());
        response.setSongCount(playlistSongs.size());

        List<SongInPlaylistResponse> songs = playlistSongs.stream()
                .map(ps->{
                    SongInPlaylistResponse songResponse = new SongInPlaylistResponse();
                    songResponse.setSongId(ps.getSong().getId());
                    songResponse.setTitle(ps.getSong().getTitle());
                    songResponse.setArtist(ps.getSong().getArtist());
                    songResponse.setAlbum(ps.getSong().getAlbum());
                    // same absolute URLs as SongResponse, so the player can use them directly
                    songResponse.setSongUrl(withBaseUrl(baseUrl, ps.getSong().getSongUrl()));
                    songResponse.setImageUrl(withBaseUrl(baseUrl, ps.getSong().getImageUrl()));
                    songResponse.setPosition(ps.getPosition());
                    songResponse.setAddedAt(ps.getAddedAt());
                    return songResponse;
                }).toList();
        response.setSongs(songs);
        return response;
    }

    private static String withBaseUrl(String baseUrl, String path) {
        return path != null ? baseUrl + path : null;
    }
}
