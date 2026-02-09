package com.telecom.usage.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usage_session", columnNames = {"session_id"})
}, indexes = {
    @Index(name = "idx_usage_contrat", columnList = "contrat_id"),
    @Index(name = "idx_usage_subscription", columnList = "subscription_id"),
    @Index(name = "idx_usage_date", columnList = "date_usage"),
    @Index(name = "idx_usage_type", columnList = "usage_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    /**
     * Reference to the subscription (primary relationship)
     * Usage is always linked to a subscription, not directly to a customer
     */
    @Column(name = "subscription_id")
    private Long subscriptionId;
    
    /**
     * Legacy: Contract ID - kept for backward compatibility
     * @deprecated Use subscriptionId instead
     */
    @Column(name = "contrat_id", nullable = false)
    private Long contratId;
    
    @Column(name = "service_id", nullable = false)
    private Long serviceId;
    
    /**
     * Type of usage: VOICE, SMS, DATA, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", length = 20)
    private UsageType usageType;
    
    /**
     * Usage quantity - interpretation depends on usageType:
     * - VOICE: duration in seconds
     * - SMS: count of messages
     * - DATA: volume in bytes
     */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantite;
    
    /**
     * Unit of measurement (seconds, count, bytes, KB, MB, GB)
     */
    @Column(name = "unit", length = 20)
    private String unit;
    
    @Column(name = "prix_unitaire", precision = 10, scale = 4)
    private BigDecimal prixUnitaire;
    
    @Column(name = "montant_total", precision = 15, scale = 4)
    private BigDecimal montantTotal;
    
    @Column(name = "date_usage", nullable = false)
    private LocalDateTime dateUsage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Source system that generated this CDR
     */
    @Column(name = "cdr_source", length = 50)
    private String cdrSource;
    
    /**
     * Original raw CDR data for audit purposes
     */
    @Column(name = "cdr_raw_data", columnDefinition = "TEXT")
    @Lob
    private String cdrRawData;
    
    /**
     * Called party number (for voice/SMS)
     */
    @Column(name = "called_number", length = 50)
    private String calledNumber;
    
    /**
     * Calling party number
     */
    @Column(name = "calling_number", length = 50)
    private String callingNumber;
    
    /**
     * Cell/tower ID for location tracking
     */
    @Column(name = "cell_id", length = 50)
    private String cellId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private UsageStatus status = UsageStatus.RECORDED;
    
    private boolean rated;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        rated = false;
        if (status == null) {
            status = UsageStatus.RECORDED;
        }
        // Sync subscriptionId with contratId for backward compatibility
        if (subscriptionId == null && contratId != null) {
            subscriptionId = contratId;
        }
        if (contratId == null && subscriptionId != null) {
            contratId = subscriptionId;
        }
    }
    
    public void rate(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        this.montantTotal = quantite.multiply(prixUnitaire);
        this.rated = true;
        this.status = UsageStatus.RATED;
    }
    
    public enum UsageStatus {
        RECORDED,      // Received and validated
        NORMALIZED,    // Parsed and cleaned
        PUBLISHED,     // Event published to Kafka
        RATED,         // Rating applied
        BILLED         // Included in invoice
    }
}
