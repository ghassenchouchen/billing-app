package com.telecom.usage.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for recorded usage
 */
public record RecordUsageResponse(
    Long id,
    String sessionId,
    Long contratId,
    Long serviceId,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    LocalDateTime dateUsage,
    String cdrSource,
    String status,
    boolean rated,
    String message,
    boolean duplicate
) {}
