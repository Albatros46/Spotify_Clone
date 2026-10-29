package com.muzikApp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayListRequest {
    @NotBlank(message = "Playlist name is required.")
    @Size(max = 100, message = "Playlist name must not exceed 100 characters.")
    private String name;
    @Size(max = 500, message = "Description must not exceed 500 characters.")
    private String description;
    /** Boolean (not boolean): Lombok then names the accessors getIsPublic/setIsPublic -> JSON "isPublic". */
    private Boolean isPublic;
}
