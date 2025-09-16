package dev.marketplace.marketplace.exceptions;

public class ListingLimitExceededException extends RuntimeException {
    public ListingLimitExceededException(String message) {
        super(message);
    }
}

