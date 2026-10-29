package com.muzikApp.dto.response;

import com.muzikApp.entities.Playlist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayListResponse {
    private Long id;
    private String name;
    private String description;
    /**
     * Boolean, not boolean: with a primitive Lombok generates isPublic()/setPublic() and the JSON
     * field would be called "public" instead of "isPublic".
     */
    private Boolean isPublic;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long appUserId;
    private String appUserName;

    public static PlayListResponse fromEntity(Playlist  playlist,String baseUrl) {
        PlayListResponse response = new PlayListResponse();
        response.setId(playlist.getId());
        response.setName(playlist.getName());
        response.setDescription(playlist.getDescription());
        response.setIsPublic(playlist.isPublic());
        response.setImageUrl(playlist.getImageUrl() != null ? baseUrl + playlist.getImageUrl() : null);
        response.setCreatedAt(playlist.getCreatedAt());
        response.setUpdatedAt(playlist.getUpdatedAt());
        response.setAppUserId(playlist.getAppUser().getId());
        response.setAppUserName(playlist.getAppUser().getName());
        return response;
    }
}
