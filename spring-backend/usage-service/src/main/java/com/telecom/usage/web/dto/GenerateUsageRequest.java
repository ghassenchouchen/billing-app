package com.telecom.usage.web.dto;

import java.util.List;

public record GenerateUsageRequest(
    Long abonnementId,
    List<Long> serviceIds
) {}
