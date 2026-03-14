package com.telecom.usage.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateUsageRequest(
    @NotNull(message = "Abonnement ID is required")
    Long abonnementId,
    
    @NotNull(message = "Service ID is required")
    Long serviceId,
    
    @NotNull(message = "Quantite is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantite must be greater than 0")
    BigDecimal quantite,
    
    @NotNull(message = "Date usage is required")
    LocalDateTime dateUsage
) {}
