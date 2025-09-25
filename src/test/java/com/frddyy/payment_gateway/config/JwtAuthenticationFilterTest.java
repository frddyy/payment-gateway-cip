package com.frddyy.payment_gateway.config;

import com.frddyy.payment_gateway.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        // Clear SecurityContext sebelum tiap test
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_shouldContinueFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_invalidAuthorizationHeader_shouldContinueFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_validToken_shouldSetAuthentication() throws Exception {
        String token = "Bearer valid.jwt.token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn(token);
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn(username);
        when(jwtService.isTokenValid("valid.jwt.token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(username);
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void doFilterInternal_invalidToken_shouldNotSetAuthentication() throws Exception {
        String token = "Bearer invalid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn(token);
        when(jwtService.extractUsername("invalid.jwt.token")).thenReturn("testuser");
        when(jwtService.isTokenValid("invalid.jwt.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
