package com.muzikApp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongRequest {
    @NotBlank(message = "Title is required.")
    @Size( max = 100, message = "Title must not exceed 100 characters.")
    private String title;
    @NotBlank(message = "Artist is required.")
    @Size( max = 100, message = "Artist must not exceed 100 characters.")
    private String artist;
    @NotBlank(message = "Album is required.")
    @Size( max = 100, message = "Album must not exceed 100 characters.")
    private String album;
}
