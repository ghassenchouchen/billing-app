package com.telecom.usage.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateUsageRequest(
    Long abonnementId,
    Long serviceId,
    BigDecimal quantite,
    LocalDateTime dateUsage
) {}
