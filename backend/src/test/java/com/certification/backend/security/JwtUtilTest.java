package com.certification.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "certification-system-secret-key-2026-eeas-jwt-hs256";

    @Test
    void generateTokenCanBeParsedForUsernameAndRole() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        String token = jwtUtil.generateToken("teacher01", "teacher");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("teacher01");
        assertThat(jwtUtil.getRole(token)).isEqualTo("teacher");
    }

    @Test
    void parseClaimsReturnsPayloadForValidToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        Claims claims = jwtUtil.parseClaims(jwtUtil.generateToken("admin", "admin"));

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get("role", String.class)).isEqualTo("admin");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void invalidTokenReturnsNullValuesAndFailsValidation() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        assertThat(jwtUtil.parseClaims("not-a-jwt")).isNull();
        assertThat(jwtUtil.getUsername("not-a-jwt")).isNull();
        assertThat(jwtUtil.getRole("not-a-jwt")).isNull();
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void expiredTokenFailsValidation() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1L);

        String token = jwtUtil.generateToken("student01", "student");

        assertThat(jwtUtil.parseClaims(token)).isNull();
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
