package com.muzikApp.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Typed access to the app.* settings. Invalid or missing values stop the app at startup. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String baseUrl,
        /** Allowed CORS origins, comma separated: http://localhost:5173,http://127.0.0.1:5173 */
        @NotEmpty List<@NotBlank String> frontendUrls,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Storage storage) {

    public record Jwt(
            @NotBlank String secret,
            @DefaultValue("30m") Duration accessTokenExpiration,
            @DefaultValue("7d") Duration refreshTokenExpiration) {
    }

    public record Storage(@NotBlank String songDir, @NotBlank String imageDir) {
    }
}
