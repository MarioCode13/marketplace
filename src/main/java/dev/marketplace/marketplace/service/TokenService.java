package dev.marketplace.marketplace.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility service for generating and validating email verification tokens
 */
@Service
public class TokenService {

    private static final SecureRandom random = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits

    /**
     * Generate a secure random token for email verification
     * @return A URL-safe base64 encoded token
     */
    public String generateEmailVerificationToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validate that a token has proper format
     * @param token The token to validate
     * @return True if token is valid format, false otherwise
     */
    public boolean isValidTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            // Try to decode to verify it's valid base64
            Base64.getUrlDecoder().decode(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

