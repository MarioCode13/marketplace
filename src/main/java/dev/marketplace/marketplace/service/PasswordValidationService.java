package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for validating password strength
 */
@Service
public class PasswordValidationService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);

    // Password strength requirements
    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/`~].*";

    /**
     * Validates password strength against multiple criteria
     * @param password the password to validate
     * @throws ValidationException if password does not meet strength requirements
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            logger.warn("Password validation failed: password is null or empty");
            throw new ValidationException("Password is required");
        }

        StringBuilder errors = new StringBuilder();

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            errors.append("Password must be at least ").append(MIN_LENGTH).append(" characters long. ");
        }

        // Check for uppercase letter
        if (!password.matches(UPPERCASE_PATTERN)) {
            errors.append("Password must contain at least one uppercase letter. ");
        }

        // Check for lowercase letter
        if (!password.matches(LOWERCASE_PATTERN)) {
            errors.append("Password must contain at least one lowercase letter. ");
        }

        // Check for digit
        if (!password.matches(DIGIT_PATTERN)) {
            errors.append("Password must contain at least one number. ");
        }

        // Check for special character
        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            errors.append("Password must contain at least one special character (!@#$%^&*()_+-=[]{};\\'\\\":,.<>?/`~). ");
        }

        if (errors.length() > 0) {
            String errorMessage = errors.toString().trim();
            logger.warn("Password validation failed: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        logger.debug("Password validation successful");
    }

    /**
     * Checks if a password meets basic requirements (shorter check for quick validation)
     * @param password the password to check
     * @return true if password meets basic requirements
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        return password.length() >= MIN_LENGTH &&
               password.matches(UPPERCASE_PATTERN) &&
               password.matches(LOWERCASE_PATTERN) &&
               password.matches(DIGIT_PATTERN) &&
               password.matches(SPECIAL_CHAR_PATTERN);
    }
}

