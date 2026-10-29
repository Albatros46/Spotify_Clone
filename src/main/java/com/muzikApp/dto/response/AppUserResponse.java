package com.muzikApp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.muzikApp.entities.AppUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
// tokens are only sent after login / register / refresh, not with every profile response
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppUserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AppUserResponse fromEntity(AppUser appUser,String accessToken,String refreshToken) {
        AppUserResponse response = new AppUserResponse();
        response.setId(appUser.getId());
        response.setName(appUser.getName());
        response.setEmail(appUser.getEmail());
        response.setRole(appUser.getRole().name());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setCreatedAt(appUser.getCreatedAt());
        response.setUpdatedAt(appUser.getUpdatedAt());
        return response;
    }

    public static AppUserResponse fromEntity(AppUser appUser) {
        return fromEntity(appUser, null, null);
    }
}
