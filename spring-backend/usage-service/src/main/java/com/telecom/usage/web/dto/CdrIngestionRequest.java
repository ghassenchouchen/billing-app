package com.telecom.usage.web.dto;

import com.telecom.usage.domain.entity.UsageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for ingesting CDR (Call Detail Record) data via REST API.
 * This represents external mediation or network systems sending usage data.
 * 
 * Design Decisions:
 * 1. sessionId is mandatory for idempotency - prevents duplicate records
 * 2. subscriptionId links usage to subscription (not customer) for proper billing
 * 3. usageType classifies the CDR for rating and reporting
 * 4. rawCdrData preserved for audit trail
 */
@Schema(description = "CDR (Call Detail Record) ingestion request from external systems")
public record CdrIngestionRequest(
    
    @Schema(
        description = "Unique session/transaction ID for idempotency. Prevents duplicate CDR ingestion.",
        example = "CDR-2026-02-05-123456-001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Session ID is required for idempotency")
    @Size(max = 100, message = "Session ID must not exceed 100 characters")
    String sessionId,
    
    @Schema(
        description = "Subscription ID that this usage belongs to. Usage is always linked to a subscription.",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Subscription ID is required")
    @Positive(message = "Subscription ID must be positive")
    Long subscriptionId,
    
    @Schema(
        description = "Service ID from catalog (determines rating)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Service ID is required")
    @Positive(message = "Service ID must be positive")
    Long serviceId,
    
    @Schema(
        description = "Type of usage: VOICE, SMS, DATA, etc.",
        example = "VOICE",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Usage type is required")
    UsageType usageType,
    
    @Schema(
        description = "Usage quantity. Interpretation depends on usageType: VOICE=seconds, SMS=count, DATA=bytes",
        example = "180.0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    BigDecimal quantity,
    
    @Schema(
        description = "Unit of measurement",
        example = "seconds",
        allowableValues = {"seconds", "count", "bytes", "KB", "MB", "GB"}
    )
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    String unit,
    
    @Schema(
        description = "Timestamp when the usage occurred",
        example = "2026-02-05T14:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Usage timestamp is required")
    LocalDateTime timestamp,
    
    @Schema(
        description = "Source system identifier that generated this CDR",
        example = "MSC-TUNIS-01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "CDR source is required")
    @Size(max = 50, message = "CDR source must not exceed 50 characters")
    String cdrSource,
    
    @Schema(
        description = "Called party number (for VOICE/SMS)",
        example = "+21698123456"
    )
    @Size(max = 50, message = "Called number must not exceed 50 characters")
    String calledNumber,
    
    @Schema(
        description = "Calling party number",
        example = "+21697654321"
    )
    @Size(max = 50, message = "Calling number must not exceed 50 characters")
    String callingNumber,
    
    @Schema(
        description = "Cell/tower ID for location tracking",
        example = "CELL-TUN-001"
    )
    @Size(max = 50, message = "Cell ID must not exceed 50 characters")
    String cellId,
    
    @Schema(
        description = "Original raw CDR data for audit purposes (JSON or CSV format)"
    )
    String rawCdrData,
    
    @Schema(
        description = "Correlation ID for distributed tracing",
        example = "corr-abc123"
    )
    String correlationId
) {
    /**
     * Backward compatible constructor for legacy systems using contratId
     */
    public static CdrIngestionRequest fromLegacy(
            String sessionId, Long contratId, Long serviceId, 
            BigDecimal quantity, LocalDateTime timestamp, String cdrSource) {
        return new CdrIngestionRequest(
            sessionId, contratId, serviceId, UsageType.VOICE, quantity, 
            "seconds", timestamp, cdrSource, null, null, null, null, null
        );
    }
}
