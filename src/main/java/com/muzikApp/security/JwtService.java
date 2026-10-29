package com.muzikApp.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.muzikApp.config.AppProperties;
import com.muzikApp.entities.AppUser;
import com.muzikApp.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and checks the two token types. Both are signed JWTs (HS256); the claim "type" keeps them
 * apart, so a refresh token cannot be used as an access token and the other way round.
 */
@Service
public class JwtService {

    static final String TYPE = "type";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";
    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a 256-bit key

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = properties.jwt().accessTokenExpiration();
        this.refreshTtl = properties.jwt().refreshTokenExpiration();
    }

    public String createAccessToken(AppUser user) {
        return create(user, ACCESS, accessTtl);
    }

    public String createRefreshToken(AppUser user) {
        return create(user, REFRESH, refreshTtl);
    }

    /** @return the user's e-mail if the token is a valid, unexpired ACCESS token */
    public String emailFromAccessToken(String token) {
        return parse(token, ACCESS).getSubject();
    }

    /** @return the user's e-mail if the token is a valid, unexpired REFRESH token */
    public String emailFromRefreshToken(String token) {
        return parse(token, REFRESH).getSubject();
    }

    private String create(AppUser user, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // two tokens issued in the same second still differ
                .subject(user.getEmail())
                .claim(TYPE, type)
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(TYPE, String.class))) {
                throw new InvalidTokenException("Wrong token type");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }
}
