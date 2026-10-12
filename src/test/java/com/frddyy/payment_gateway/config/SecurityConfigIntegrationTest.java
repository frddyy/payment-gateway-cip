package com.frddyy.payment_gateway.config;

import com.frddyy.payment_gateway.client.BillerAggregatorClient;
import com.frddyy.payment_gateway.client.CoreBankingClient;
import com.frddyy.payment_gateway.service.EventPublisher;
import com.frddyy.payment_gateway.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("security-test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EventPublisher eventPublisher;

    @MockitoBean
    private CoreBankingClient coreBankingClient;

    @MockitoBean
    private BillerAggregatorClient billerAggregatorClient;

    private static final String SECRET_KEY_BASE64 = "c9595fcec3d9ea97f6a740954ec92d5f70b100f863ba4da0f3ea58a9bfe747e3";
    private static final String DIFFERENT_KEY_BASE64 = "a9595fcec3d9ea97f6a740954ec92d5f70b100f863ba4da0f3ea58a9bfe747e3";

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request to business endpoint without token should return 401")
    void requestWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/payments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with Authorization header missing 'Bearer ' prefix should return 401")
    void requestWithoutBearerPrefix_shouldReturn401() throws Exception {
        String token = jwtService.generateToken("user1");
        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header("Authorization", "Basic " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with malformed token should return 401")
    void requestWithMalformedToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with token signed with different key should return 401")
    void requestWithWrongSignatureToken_shouldReturn401() throws Exception {
        SecretKey wrongKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(DIFFERENT_KEY_BASE64));
        String wrongSignatureToken = Jwts.builder()
                .subject("tamperedUser")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + wrongSignatureToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with expired token should return 401")
    void requestWithExpiredToken_shouldReturn401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY_BASE64));
        String expiredToken = Jwts.builder()
                .subject("expiredUser")
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 50000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with valid token to GET /api/payments/{id} should pass authentication (return 404 for non-existent ID)")
    void requestWithValidToken_shouldPassAuthentication() throws Exception {
        String validToken = jwtService.generateToken("validUser");
        mockMvc.perform(get("/api/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public endpoint /v3/api-docs should be accessible without token")
    void publicOpenApiDocs_shouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public mock endpoint /mock/corebank/debit should be accessible without token")
    void publicMockEndpoint_shouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/mock/corebank/debit")
                        .contentType("application/json")
                        .content("{\"account\":\"1234567890\",\"amount\":10000}"))
                .andExpect(status().isOk());
    }
}
