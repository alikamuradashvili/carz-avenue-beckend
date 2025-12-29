package com.carzavenue.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private final String accessSecret;
    private final String refreshSecret;
    private final int accessExpirationMinutes;
    private final int refreshExpirationDays;

    public JwtService(
            @Value("${app.jwt.secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-expiration-minutes}") int accessExpirationMinutes,
            @Value("${app.jwt.refresh-expiration-days}") int refreshExpirationDays) {
        this.accessSecret = accessSecret;
        this.refreshSecret = refreshSecret;
        this.accessExpirationMinutes = accessExpirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpirationMinutes * 60L)))
                .signWith(getSigningKey(accessSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(getRefreshExpirationSeconds())))
                .signWith(getSigningKey(refreshSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, boolean refresh) {
        return !isTokenExpired(token, refresh);
    }

    public String extractUserEmail(String token, boolean refresh) {
        return extractClaim(token, Claims::getSubject, refresh);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver, boolean refresh) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey(refresh ? refreshSecret : accessSecret))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    private boolean isTokenExpired(String token, boolean refresh) {
        Date expiration = extractClaim(token, Claims::getExpiration, refresh);
        return expiration.before(new Date());
    }

    private Key getSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret is empty");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            keyBytes = sha256(secret);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationDays * 24L * 60L * 60L;
    }

    private byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive JWT signing key", ex);
        }
    }
}
