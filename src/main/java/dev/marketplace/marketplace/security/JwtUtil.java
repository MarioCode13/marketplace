package dev.marketplace.marketplace.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import java.util.Date;
import java.util.function.Function;
import java.util.UUID;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    @PostConstruct
    public void init() {
        logger.info("JwtUtil initialized with secret: {}", secret != null ? "NOT NULL" : "NULL");
        if (secret != null && secret.length() > 8) {
            logger.info("JWT secret loaded: {}...{} (length: {})", 
                secret.substring(0, 8), 
                secret.substring(secret.length() - 8), 
                secret.length());
        } else {
            logger.warn("JWT secret is null or too short: {}", secret);
        }
    }

    public String generateToken(String email, String role, UUID userId) {
        logger.debug("Generating JWT token for user: {} (ID: {}, Role: {})", email, userId, role);
        logger.debug("Using JWT secret: {}", secret);
        try {
            String token = Jwts.builder()
                    .subject(email)
                    .claim("role", role)
                    .claim("userId", userId.toString())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(getSigningKey(), Jwts.SIG.HS256)
                    .compact();
            logger.debug("JWT token generated successfully for user: {}", email);
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token for user: {}", email, e);
            throw e;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public UUID extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", UUID.class));
    }

    private SecretKey getSigningKey() {
        logger.debug("Decoding JWT secret from base64: {}", secret);
        try {
            byte[] decodedBytes = Decoders.BASE64.decode(secret);
            logger.debug("Successfully decoded {} bytes from base64", decodedBytes.length);
            return Keys.hmacShaKeyFor(decodedBytes);
        } catch (Exception e) {
            logger.error("Failed to decode JWT secret from base64: {}", secret, e);
            throw e;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .json(new JacksonDeserializer<>())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .json(new JacksonDeserializer<>())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
