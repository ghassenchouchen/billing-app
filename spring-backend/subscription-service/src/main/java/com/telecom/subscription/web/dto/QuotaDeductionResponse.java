package com.telecom.subscription.web.dto;

import com.telecom.subscription.domain.entity.Quota;

import java.math.BigDecimal;

/**
 * Response from a quota deduction attempt.
 */
public record QuotaDeductionResponse(
    boolean success,
    String message,
    Quota.QuotaType usageType,
    BigDecimal remaining,
    BigDecimal deducted,
    BigDecimal total,
    Long subscriptionId
) {
    public static QuotaDeductionResponse success(Quota quota, BigDecimal deducted) {
        return new QuotaDeductionResponse(
            true,
            "Quota deducted successfully",
            quota.getQuotaType(),
            quota.getRemainingAmount(),
            deducted,
            quota.getTotalAmount(),
            quota.getAbonnement().getId()
        );
    }

    public static QuotaDeductionResponse insufficientQuota(Quota quota, BigDecimal requested) {
        return new QuotaDeductionResponse(
            false,
            "Insufficient quota. Remaining: " + quota.getRemainingAmount() + ", requested: " + requested,
            quota.getQuotaType(),
            quota.getRemainingAmount(),
            BigDecimal.ZERO,
            quota.getTotalAmount(),
            quota.getAbonnement().getId()
        );
    }

    public static QuotaDeductionResponse noQuotaConfigured(Quota.QuotaType usageType, Long subscriptionId) {
        return new QuotaDeductionResponse(
            false,
            "No " + usageType + " quota configured for subscription " + subscriptionId,
            usageType,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            subscriptionId
        );
    }
}
