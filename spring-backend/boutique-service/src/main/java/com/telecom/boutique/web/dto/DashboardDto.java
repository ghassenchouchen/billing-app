package com.telecom.boutique.web.dto;

import java.math.BigDecimal;
import java.util.Map;


public record DashboardDto(
    BigDecimal revenueToday,
    long contractsThisMonth,
    long contractTarget,
    long simAvailable,
    long simLowStock,
    Map<String, Long> simByType
) {}
