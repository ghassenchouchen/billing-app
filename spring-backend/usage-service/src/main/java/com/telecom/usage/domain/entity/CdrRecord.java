package com.telecom.usage.domain.entity;

/**
 * Domain model representing a parsed CDR(call detail record) record from CSV
 */
public record CdrRecord(
    String externalId,           // Unique identifier from external system (e.g., CDR_2024_001)
    Long contratId,              // Contract ID from CDR
    Long serviceId,              // Service ID from CDR
    String quantity,             // Quantity as string (to be parsed)
    String dateUsage,            // Date usage as string (to be parsed)
    String cdrSource,            // Source system name
    String rawLine               // Original raw line for auditing
) {
    
    /**
     * Validate CDR record has required fields
     */
    public boolean isValid() {
        return externalId != null && !externalId.isEmpty() &&
               contratId != null &&
               serviceId != null &&
               quantity != null && !quantity.isEmpty() &&
               dateUsage != null && !dateUsage.isEmpty();
    }
    
    /**
     * Get validation error message if invalid
     */
    public String getValidationError() {
        if (externalId == null || externalId.isEmpty()) {
            return "External ID is required";
        }
        if (contratId == null) {
            return "Contract ID is required";
        }
        if (serviceId == null) {
            return "Service ID is required";
        }
        if (quantity == null || quantity.isEmpty()) {
            return "Quantity is required";
        }
        if (dateUsage == null || dateUsage.isEmpty()) {
            return "Date usage is required";
        }
        return null;
    }
}
