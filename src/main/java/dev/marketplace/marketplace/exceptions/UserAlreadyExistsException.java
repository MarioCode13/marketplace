package dev.marketplace.marketplace.exceptions;

public class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String message) {
        super(message, "USER_ALREADY_EXISTS");
    }
} 