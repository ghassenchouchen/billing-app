package com.telecom.usage.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usage_session", columnNames = {"session_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "contrat_id", nullable = false)
    private Long contratId;
    
    @Column(name = "service_id", nullable = false)
    private Long serviceId;
    
    @Column(nullable = false)
    private BigDecimal quantite;
    
    @Column(name = "prix_unitaire")
    private BigDecimal prixUnitaire;
    
    @Column(name = "montant_total")
    private BigDecimal montantTotal;
    
    @Column(name = "date_usage", nullable = false)
    private LocalDateTime dateUsage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "cdr_source")
    private String cdrSource;
    
    @Column(name = "cdr_raw_data")
    @Lob
    private String cdrRawData;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UsageStatus status = UsageStatus.RECORDED;
    
    private boolean rated;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        rated = false;
        if (status == null) {
            status = UsageStatus.RECORDED;
        }
    }
    
    public void rate(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        this.montantTotal = quantite.multiply(prixUnitaire);
        this.rated = true;
    }
    
    public enum UsageStatus {
        RECORDED,      // Received and validated
        NORMALIZED,    // Parsed and cleaned
        PUBLISHED,     // Event published to Kafka
        RATED,         // Rating applied
        BILLED         // Included in invoice
    }
}
