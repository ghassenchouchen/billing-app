package com.telecom.boutique.domain.exception;

/**
 * Thrown when a SIM card cannot be found by ICCID.
 */
public class SimNotFoundException extends RuntimeException {
    public SimNotFoundException(String iccid) {
        super("SIM card not found with ICCID: " + iccid);
    }
}
