package com.telecom.billing.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "usage-service", url = "${services.usage-url}")
public interface UsageClient {
    
    @GetMapping("/api/usage/subscription/{subscriptionId}")
    List<UsageRecordDto> getUsageBySubscription(@PathVariable("subscriptionId") Long subscriptionId);
    
    @GetMapping("/api/usage/subscription/{subscriptionId}/unbilled")
    List<UsageRecordDto> getUnbilledUsage(@PathVariable("subscriptionId") Long subscriptionId);
    
    @GetMapping("/api/usage/subscription/{subscriptionId}/period")
    List<UsageRecordDto> getUsageByPeriod(
        @PathVariable("subscriptionId") Long subscriptionId,
        @RequestParam("startDate") String startDate,
        @RequestParam("endDate") String endDate
    );
    
    record UsageRecordDto(
        Long id,
        Long abonnementId,
        String type,
        Integer quantite,
        String unite,
        BigDecimal montant,
        String dateUsage,
        boolean billed
    ) {}
}
