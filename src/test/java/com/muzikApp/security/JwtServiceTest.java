package com.muzikApp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.muzikApp.config.AppProperties;
import com.muzikApp.entities.AppUser;
import com.muzikApp.entities.Role;
import com.muzikApp.exception.InvalidTokenException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private final JwtService jwt = service(SECRET, Duration.ofMinutes(30));
    private final AppUser user = user("anna@example.com");

    @Test
    void accessTokenCarriesTheEmail() {
        assertThat(jwt.emailFromAccessToken(jwt.createAccessToken(user))).isEqualTo("anna@example.com");
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() {
        String refresh = jwt.createRefreshToken(user);

        assertThatThrownBy(() -> jwt.emailFromAccessToken(refresh)).isInstanceOf(InvalidTokenException.class);
        assertThat(jwt.emailFromRefreshToken(refresh)).isEqualTo("anna@example.com");
    }

    @Test
    void accessTokenIsNotAcceptedAsRefreshToken() {
        String access = jwt.createAccessToken(user);

        assertThatThrownBy(() -> jwt.emailFromRefreshToken(access)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.createAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwt.emailFromAccessToken(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = service("another-secret-another-secret-0123456789xyz", Duration.ofMinutes(30));

        assertThatThrownBy(() -> jwt.emailFromAccessToken(other.createAccessToken(user)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiring = service(SECRET, Duration.ofSeconds(-1));

        assertThatThrownBy(() -> jwt.emailFromAccessToken(expiring.createAccessToken(user)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void twoTokensForTheSameUserDiffer() {
        assertThat(jwt.createRefreshToken(user)).isNotEqualTo(jwt.createRefreshToken(user));
    }

    @Test
    void tooShortSecretStopsTheApplication() {
        assertThatThrownBy(() -> service("short", Duration.ofMinutes(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private static JwtService service(String secret, Duration ttl) {
        return new JwtService(new AppProperties("http://localhost", "http://localhost:4200",
                new AppProperties.Jwt(secret, ttl, ttl), new AppProperties.Storage("s", "i")));
    }

    private static AppUser user(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setRole(Role.USER);
        return user;
    }
}
