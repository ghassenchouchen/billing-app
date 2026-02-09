package com.telecom.usage.web.dto;

import com.telecom.usage.domain.entity.UsageRecord;
import com.telecom.usage.domain.entity.UsageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for CDR ingestion results
 */
@Schema(description = "Response from CDR ingestion operation")
public record CdrIngestionResponse(
    
    @Schema(description = "Internal usage record ID", example = "12345")
    Long usageId,
    
    @Schema(description = "Session ID used for idempotency", example = "CDR-2026-02-05-123456-001")
    String sessionId,
    
    @Schema(description = "Subscription ID", example = "1001")
    Long subscriptionId,
    
    @Schema(description = "Service ID", example = "1")
    Long serviceId,
    
    @Schema(description = "Type of usage", example = "VOICE")
    UsageType usageType,
    
    @Schema(description = "Usage quantity", example = "180.0")
    BigDecimal quantity,
    
    @Schema(description = "Unit of measurement", example = "seconds")
    String unit,
    
    @Schema(description = "Timestamp of usage", example = "2026-02-05T14:30:00")
    LocalDateTime timestamp,
    
    @Schema(description = "Record creation time", example = "2026-02-05T14:35:00")
    LocalDateTime createdAt,
    
    @Schema(description = "CDR source system", example = "MSC-TUNIS-01")
    String cdrSource,
    
    @Schema(description = "Current processing status", example = "RECORDED")
    String status,
    
    @Schema(description = "Whether this was a duplicate request", example = "false")
    boolean duplicate,
    
    @Schema(description = "Response message", example = "CDR successfully recorded")
    String message,
    
    @Schema(description = "Correlation ID for tracing", example = "corr-abc123")
    String correlationId
) {
    /**
     * Factory method to create response from entity
     */
    public static CdrIngestionResponse fromEntity(UsageRecord usage, boolean duplicate, String message, String correlationId) {
        return new CdrIngestionResponse(
            usage.getId(),
            usage.getSessionId(),
            usage.getSubscriptionId() != null ? usage.getSubscriptionId() : usage.getContratId(),
            usage.getServiceId(),
            usage.getUsageType(),
            usage.getQuantite(),
            usage.getUnit(),
            usage.getDateUsage(),
            usage.getCreatedAt(),
            usage.getCdrSource(),
            usage.getStatus().name(),
            duplicate,
            message,
            correlationId
        );
    }
    
    /**
     * Factory method for successful ingestion
     */
    public static CdrIngestionResponse success(UsageRecord usage, String correlationId) {
        return fromEntity(usage, false, "CDR successfully recorded", correlationId);
    }
    
    /**
     * Factory method for duplicate detection
     */
    public static CdrIngestionResponse duplicate(UsageRecord existing, String correlationId) {
        return fromEntity(existing, true, "CDR already exists with this session ID", correlationId);
    }
}
