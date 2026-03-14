package com.telecom.boutique.web.dto;

import java.time.LocalDateTime;

public record StockSimDto(
    Long id,
    String iccid,
    String imsi,
    String msisdn,
    String simType,
    String status,
    Long boutiqueId,
    Long assignedToClientId,
    LocalDateTime assignedAt,
    LocalDateTime createdAt
) {}
