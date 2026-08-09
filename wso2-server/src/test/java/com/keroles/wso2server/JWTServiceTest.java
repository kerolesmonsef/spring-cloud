package com.keroles.wso2server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JWTServiceTest {
    private final JWTService jwtService = new JWTService(
            "learning-only-secret-key-with-at-least-32-bytes", 3_600_000L);

    @Test
    void createsAndValidatesTokenForUser() {
        String token = jwtService.generateToken("mbank");

        assertThat(jwtService.extractUsername(token)).isEqualTo("mbank");
        assertThat(jwtService.isTokenValid(token, "mbank")).isTrue();
        assertThat(jwtService.isTokenValid(token, "other-user")).isFalse();
    }
}
