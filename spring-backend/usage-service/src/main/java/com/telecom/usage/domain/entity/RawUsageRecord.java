package com.telecom.usage.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_usage_record", indexes = {
    @Index(name = "idx_cdr_source", columnList = "cdr_source"),
    @Index(name = "idx_external_id", columnList = "external_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawUsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * External system identifier (e.g., CDR_20240102_001, MEDIATION_BATCH_123)
     */
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;
    
    /**
     * Source of the CDR (e.g., "file-batch", "api-mediation", "network-system")
     */
    @Column(name = "cdr_source", nullable = false)
    private String cdrSource;
    
    /**
     * Raw CSV line or JSON data before parsing
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    @Lob
    private String rawData;
    
    /**
     * Session ID for batch processing idempotency
     */
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    /**
     * Processing status: RECEIVED, VALIDATED, NORMALIZED, MAPPED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CdrStatus status = CdrStatus.RECEIVED;
    
    /**
     * Error message if validation or processing failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Reference to the processed UsageRecord if successful
     */
    @Column(name = "usage_record_id")
    private Long usageRecordId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = CdrStatus.RECEIVED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum CdrStatus {
        RECEIVED,      // Initial state - raw data received
        VALIDATED,     // Format and fields validated
        NORMALIZED,    // Data cleaned and standardized
        MAPPED,        // Mapped to UsageRecord
        FAILED,        // Processing failed
        DUPLICATE      // Duplicate record detected
    }
}
