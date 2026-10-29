package com.muzikApp.dto.response;

import com.muzikApp.entities.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongResponse {

    private Long id;
    private String title;
    private String artist;
    private String album;
    private String songUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Long appUserId;
    private String appUserName;

    public static SongResponse fromEntity(Song song,String baseUrl){
        SongResponse response = new SongResponse();
        response.setId(song.getId());
        response.setTitle(song.getTitle());
        response.setArtist(song.getArtist());
        response.setAlbum(song.getAlbum());
        response.setSongUrl(song.getSongUrl()!=null ?   baseUrl + song.getSongUrl() : null);
        response.setImageUrl(song.getImageUrl()!=null ?   baseUrl + song.getImageUrl() : null);
        response.setCreatedAt(song.getCreatedAt());
        response.setAppUserId(song.getAppUser().getId());
        response.setAppUserName(song.getAppUser().getName());

        return response;
    }
}
