package com.telecom.subscription.web.dto;

import com.telecom.subscription.domain.entity.Quota;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only DTO for quota information.
 */
public record QuotaDto(
    Long id,
    Long subscriptionId,
    Quota.QuotaType quotaType,
    BigDecimal totalAmount,
    BigDecimal remainingAmount,
    BigDecimal usedAmount,
    BigDecimal usagePercentage,
    String unit,
    boolean exhausted,
    LocalDateTime updatedAt
) {
    public static QuotaDto fromEntity(Quota quota) {
        return new QuotaDto(
            quota.getId(),
            quota.getAbonnement().getId(),
            quota.getQuotaType(),
            quota.getTotalAmount(),
            quota.getRemainingAmount(),
            quota.getUsedAmount(),
            quota.getUsagePercentage(),
            quota.getUnit(),
            quota.isExhausted(),
            quota.getUpdatedAt()
        );
    }
}
