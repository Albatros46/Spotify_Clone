package com.muzikApp.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void hashIsSha256HexAndNotTheToken() {
        String hash = TokenHasher.sha256("my-refresh-token");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+").doesNotContain("my-refresh-token");
    }

    @Test
    void matchesOnlyTheSameToken() {
        String stored = TokenHasher.sha256("token-1");

        assertThat(TokenHasher.matches("token-1", stored)).isTrue();
        assertThat(TokenHasher.matches("token-2", stored)).isFalse();
        assertThat(TokenHasher.matches("token-1", null)).isFalse();
        assertThat(TokenHasher.matches(null, stored)).isFalse();
    }
}
