package com.frddyy.payment_gateway.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Mock property values
    private String secretKey = "c9595fcec3d9ea97f6a740954ec92d5f70b100f863ba4da0f3ea58a9bfe747e3";
    private long jwtExpiration = 86400000; // 24 hours in milliseconds
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Mocking the values from application.properties
        jwtService.setSecretKeyString(secretKey);
        jwtService.setJwtExpiration(jwtExpiration);
    }

    @Test
    void testGenerateToken() {
        // Given
        String username = "testuser";

        // When
        String token = jwtService.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).startsWith("eyJ");  // Token JWT biasanya dimulai dengan "eyJ"
    }

    @Test
    void testExtractUsername() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void testIsTokenValid_ValidToken() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void testIsTokenValid_InvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void testExtractClaim() {
        // Given
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // When
        String extractedUsername = jwtService.extractClaim(token, Claims::getSubject);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }
}
