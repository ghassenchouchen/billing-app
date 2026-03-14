package com.telecom.boutique.domain.exception;

/**
 * Thrown when a unique constraint would be violated (e.g. duplicate boutique code).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
