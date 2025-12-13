package com.loganomaly.detector.auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", 
            "test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L); // 1 hour
    }

    @Test
    void shouldGenerateToken() {
        // Given
        String username = "testuser";
        Long userId = 1L;

        // When
        String token = jwtService.generateToken(username, userId);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username, 1L);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        Long userId = 123L;
        String token = jwtService.generateToken("testuser", userId);

        // When
        Long extractedUserId = jwtService.extractUserId(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = jwtService.generateToken("testuser", 1L);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldValidateTokenWithUsername() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username, 1L);

        // When
        Boolean isValid = jwtService.validateToken(token, username);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldRejectTokenWithWrongUsername() {
        // Given
        String token = jwtService.generateToken("testuser", 1L);

        // When
        Boolean isValid = jwtService.validateToken(token, "wronguser");

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldNotBeExpired() {
        // Given
        String token = jwtService.generateToken("testuser", 1L);

        // When
        Boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void shouldExtractExpiration() {
        // Given
        String token = jwtService.generateToken("testuser", 1L);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date())); // Should be in the future
    }

    @Test
    void shouldHaveCorrectExpirationTime() {
        // Given
        long currentTime = System.currentTimeMillis();
        String token = jwtService.generateToken("testuser", 1L);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        long expirationTime = expiration.getTime();
        long expectedExpiration = currentTime + 3600000L; // 1 hour
        
        // Allow 1 second tolerance for test execution time
        assertTrue(Math.abs(expirationTime - expectedExpiration) < 1000);
    }
}

