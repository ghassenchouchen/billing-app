package com.telecom.boutique.domain.exception;

/**
 * Thrown when a boutique cannot be found by ID or code.
 */
public class BoutiqueNotFoundException extends RuntimeException {
    public BoutiqueNotFoundException(Long id) {
        super("Boutique not found with id: " + id);
    }

    public BoutiqueNotFoundException(String code) {
        super("Boutique not found with code: " + code);
    }
}
