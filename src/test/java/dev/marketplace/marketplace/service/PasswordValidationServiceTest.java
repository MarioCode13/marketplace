package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordValidationServiceTest {

    private PasswordValidationService passwordValidationService;

    @BeforeEach
    public void setUp() {
        passwordValidationService = new PasswordValidationService();
    }

    // ===== VALID PASSWORD TESTS =====
    @Test
    public void testValidPassword() {
        String validPassword = "ValidPass123!";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(validPassword));
        assertTrue(passwordValidationService.isPasswordStrong(validPassword));
    }

    @Test
    public void testValidPasswordWithMultipleSpecialChars() {
        String validPassword = "ValidPass@123#";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(validPassword));
        assertTrue(passwordValidationService.isPasswordStrong(validPassword));
    }

    @Test
    public void testValidLongPassword() {
        String validPassword = "VeryLongValidPassword123!@#";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(validPassword));
        assertTrue(passwordValidationService.isPasswordStrong(validPassword));
    }

    // ===== INVALID PASSWORD TESTS - TOO SHORT =====
    @Test
    public void testPasswordTooShort() {
        String shortPassword = "Pass1!";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(shortPassword)
        );
        assertTrue(exception.getMessage().contains("at least 8 characters"));
        assertFalse(passwordValidationService.isPasswordStrong(shortPassword));
    }

    // ===== INVALID PASSWORD TESTS - MISSING UPPERCASE =====
    @Test
    public void testPasswordMissingUppercase() {
        String noUpperPassword = "validpass123!";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(noUpperPassword)
        );
        assertTrue(exception.getMessage().contains("uppercase letter"));
        assertFalse(passwordValidationService.isPasswordStrong(noUpperPassword));
    }

    // ===== INVALID PASSWORD TESTS - MISSING LOWERCASE =====
    @Test
    public void testPasswordMissingLowercase() {
        String noLowerPassword = "VALIDPASS123!";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(noLowerPassword)
        );
        assertTrue(exception.getMessage().contains("lowercase letter"));
        assertFalse(passwordValidationService.isPasswordStrong(noLowerPassword));
    }

    // ===== INVALID PASSWORD TESTS - MISSING DIGIT =====
    @Test
    public void testPasswordMissingDigit() {
        String noDigitPassword = "ValidPass!";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(noDigitPassword)
        );
        assertTrue(exception.getMessage().contains("number"));
        assertFalse(passwordValidationService.isPasswordStrong(noDigitPassword));
    }

    // ===== INVALID PASSWORD TESTS - MISSING SPECIAL CHAR =====
    @Test
    public void testPasswordMissingSpecialChar() {
        String noSpecialPassword = "ValidPass123";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(noSpecialPassword)
        );
        assertTrue(exception.getMessage().contains("special character"));
        assertFalse(passwordValidationService.isPasswordStrong(noSpecialPassword));
    }

    // ===== INVALID PASSWORD TESTS - MULTIPLE ISSUES =====
    @Test
    public void testPasswordWithMultipleIssues() {
        String badPassword = "pass"; // too short, no uppercase, no digit, no special
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(badPassword)
        );
        String message = exception.getMessage();
        assertTrue(message.contains("8 characters"));
        assertTrue(message.contains("uppercase letter"));
        assertTrue(message.contains("number"));
        assertTrue(message.contains("special character"));
    }

    // ===== NULL AND EMPTY PASSWORD TESTS =====
    @Test
    public void testNullPassword() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength(null)
        );
        assertTrue(exception.getMessage().contains("required"));
    }

    @Test
    public void testEmptyPassword() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> passwordValidationService.validatePasswordStrength("")
        );
        assertTrue(exception.getMessage().contains("required"));
    }

    @Test
    public void testIsPasswordStrongWithNull() {
        assertFalse(passwordValidationService.isPasswordStrong(null));
    }

    @Test
    public void testIsPasswordStrongWithEmpty() {
        assertFalse(passwordValidationService.isPasswordStrong(""));
    }

    // ===== EDGE CASES - SPECIAL CHARACTERS =====
    @Test
    public void testPasswordWithDash() {
        String passwordWithDash = "ValidPass1-";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(passwordWithDash));
        assertTrue(passwordValidationService.isPasswordStrong(passwordWithDash));
    }

    @Test
    public void testPasswordWithEquals() {
        String passwordWithEquals = "ValidPass1=";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(passwordWithEquals));
        assertTrue(passwordValidationService.isPasswordStrong(passwordWithEquals));
    }

    @Test
    public void testPasswordWithParentheses() {
        String passwordWithParens = "ValidPass1(";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(passwordWithParens));
        assertTrue(passwordValidationService.isPasswordStrong(passwordWithParens));
    }

    @Test
    public void testPasswordWithBacktick() {
        String passwordWithBacktick = "ValidPass1`";
        assertDoesNotThrow(() -> passwordValidationService.validatePasswordStrength(passwordWithBacktick));
        assertTrue(passwordValidationService.isPasswordStrong(passwordWithBacktick));
    }
}

