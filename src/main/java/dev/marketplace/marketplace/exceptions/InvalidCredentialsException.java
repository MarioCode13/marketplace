package dev.marketplace.marketplace.exceptions;

public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS");
    }
} 