package com.telecom.usage.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GenerateUsageRequest(
    @NotNull(message = "Abonnement ID is required")
    Long abonnementId,
    
    @NotEmpty(message = "Service IDs list cannot be empty")
    List<Long> serviceIds
) {}
