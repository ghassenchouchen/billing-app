package com.telecom.boutique.domain.exception;

/**
 * Thrown when a SIM card cannot transition to the requested status.
 */
public class SimNotAvailableException extends RuntimeException {
    public SimNotAvailableException(String iccid, String currentStatus) {
        super("SIM " + iccid + " is not available for this operation (current status: " + currentStatus + ")");
    }
}
