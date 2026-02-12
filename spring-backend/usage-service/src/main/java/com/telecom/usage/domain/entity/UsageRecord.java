package com.telecom.usage.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_usage_session", columnNames = {"session_id"})
}, indexes = {
    @Index(name = "idx_usage_abonnement", columnList = "abonnement_id"),
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
    
    @Column(name = "subscription_id")
    private Long subscriptionId;
    
    @Column(name = "abonnement_id", nullable = false)
    private Long abonnementId;
    
    @Column(name = "service_id", nullable = false)
    private Long serviceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", length = 20)
    private UsageType usageType;
    
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantite;
    
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
    
    @Column(name = "cdr_source", length = 50)
    private String cdrSource;
    
    @Column(name = "cdr_raw_data", columnDefinition = "TEXT")
    @Lob
    private String cdrRawData;
    
    @Column(name = "called_number", length = 50)
    private String calledNumber;
    
    @Column(name = "calling_number", length = 50)
    private String callingNumber;
    
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
        if (subscriptionId == null && abonnementId != null) {
            subscriptionId = abonnementId;
        }
        if (abonnementId == null && subscriptionId != null) {
            abonnementId = subscriptionId;
        }
    }
    
    public void rate(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        this.montantTotal = quantite.multiply(prixUnitaire);
        this.rated = true;
        this.status = UsageStatus.RATED;
    }
    
    public enum UsageStatus {
        RECORDED,
        NORMALIZED,
        PUBLISHED,
        RATED,
        BILLED,
        REJECTED
    }
}
