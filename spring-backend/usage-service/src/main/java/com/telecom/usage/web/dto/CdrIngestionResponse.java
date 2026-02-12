package com.telecom.usage.web.dto;

import com.telecom.usage.domain.entity.UsageRecord;
import com.telecom.usage.domain.entity.UsageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response from CDR ingestion operation")
public record CdrIngestionResponse(
    Long usageId,
    String sessionId,
    Long subscriptionId,
    Long serviceId,
    UsageType usageType,
    BigDecimal quantity,
    String unit,
    LocalDateTime timestamp,
    LocalDateTime createdAt,
    String cdrSource,
    String status,
    boolean duplicate,
    String message,
    String correlationId
) {
    public static CdrIngestionResponse fromEntity(UsageRecord usage, boolean duplicate, String message, String correlationId) {
        return new CdrIngestionResponse(
            usage.getId(),
            usage.getSessionId(),
            usage.getSubscriptionId() != null ? usage.getSubscriptionId() : usage.getAbonnementId(),
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
    
    public static CdrIngestionResponse success(UsageRecord usage, String correlationId) {
        return fromEntity(usage, false, "CDR successfully recorded", correlationId);
    }
    
    public static CdrIngestionResponse duplicate(UsageRecord existing, String correlationId) {
        return fromEntity(existing, true, "CDR already exists with this session ID", correlationId);
    }
}
