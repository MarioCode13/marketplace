package dev.marketplace.marketplace.exceptions;

public class ValidationException extends AuthException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
} 