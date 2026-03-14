package com.telecom.subscription.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a usage quota allocated to a prepaid subscription.
 * 
 * Each subscription can have multiple quotas , one per usage type (VOICE, SMS, DATA).
 * 
 * Deduction is synchronous: usage-service calls POST /subscriptions/{id}/deduct-quota
 * before recording any prepaid usage.
 */
@Entity
@Table(name = "quota", indexes = {
    @Index(name = "idx_quota_abonnement", columnList = "abonnement_id"),
    @Index(name = "idx_quota_abonnement_type", columnList = "abonnement_id, quota_type", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_type", nullable = false, length = 20)
    private QuotaType quotaType;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal remainingAmount;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (remainingAmount == null) {
            remainingAmount = totalAmount;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasSufficientQuota(BigDecimal requestedAmount) {
        return remainingAmount.compareTo(requestedAmount) >= 0;
    }

    public boolean deduct(BigDecimal amount) {
        if (!hasSufficientQuota(amount)) {
            return false;
        }
        this.remainingAmount = this.remainingAmount.subtract(amount);
        return true;
    }

    public BigDecimal getUsedAmount() {
        return totalAmount.subtract(remainingAmount);
    }

    public BigDecimal getUsagePercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return getUsedAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean isExhausted() {
        return remainingAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    public enum QuotaType {
        VOICE,
        SMS,
        DATA
    }
}
