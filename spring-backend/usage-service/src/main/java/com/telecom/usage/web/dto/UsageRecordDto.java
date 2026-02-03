package com.telecom.usage.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UsageRecordDto(
    Long id,
    Long contratId,
    Long serviceId,
    BigDecimal quantite,
    BigDecimal prixUnitaire,
    BigDecimal montantTotal,
    LocalDateTime dateUsage,
    boolean rated
) {}
