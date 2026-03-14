package com.telecom.boutique.domain.exception;

/**
 * Thrown when a customer cannot be resolved by id before SIM lifecycle operations.
 */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long customerId) {
        super("Customer not found with id: " + customerId);
    }
}
