package com.frddyy.payment_gateway.service;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // Ambil secret key dari application.properties
    // Penting: Key ini harus sangat rahasia dan panjang (minimal 256-bit)
    @Value("${application.security.jwt.secret-key}")
    private String secretKeyString;

    // Ambil expiration time dari application.properties
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token) {
    try {
        extractAllClaims(token);
        return true;
    } catch (Exception e) {
        return false;
    }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Menambahkan setter untuk unit testing
    public void setSecretKeyString(String secretKeyString) {
        this.secretKeyString = secretKeyString;
    }

    public void setJwtExpiration(long jwtExpiration) {
        this.jwtExpiration = jwtExpiration;
    }

}