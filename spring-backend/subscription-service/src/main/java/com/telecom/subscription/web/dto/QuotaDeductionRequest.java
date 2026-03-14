package com.telecom.subscription.web.dto;

import com.telecom.subscription.domain.entity.Quota;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request to deduct quota from a prepaid subscription.
 * Sent by usage-service when processing a prepaid CDR.
 */
public record QuotaDeductionRequest(

    @NotNull(message = "Usage type is required")
    Quota.QuotaType usageType,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    BigDecimal quantity
) {}
