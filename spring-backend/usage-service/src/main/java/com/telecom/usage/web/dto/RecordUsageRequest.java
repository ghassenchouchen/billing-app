package com.telecom.usage.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordUsageRequest(
    @NotBlank(message = "Session ID is required for idempotency")
    String sessionId,
    
    @NotNull(message = "Subscription ID is required")
    @Positive(message = "Subscription ID must be positive")
    Long abonnementId,
    
    @NotNull(message = "Service ID is required")
    @Positive(message = "Service ID must be positive")
    Long serviceId,
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    BigDecimal quantity,
    
    @NotNull(message = "Usage date is required")
    LocalDateTime dateUsage,
    
    @NotBlank(message = "CDR source is required")
    String cdrSource,
    
    String correlationId,
    String rawCdrData
) {}
