package com.telecom.usage.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for subscription-service.
 * Used to:
 * 1. Look up subscription details (to get clientId)
 * 2. Deduct quota for prepaid usage
 */
@FeignClient(name = "subscription-service", url = "${services.subscription-url}")
public interface SubscriptionClient {

    @GetMapping("/subscriptions/{id}")
    SubscriptionDto getSubscription(@PathVariable("id") Long id);

    /**
     * Deduct quota from a prepaid subscription.
     * Returns success/failure with remaining quota.
     */
    @PostMapping("/subscriptions/{id}/deduct-quota")
    QuotaDeductionResult deductQuota(@PathVariable("id") Long id, @RequestBody QuotaDeductionPayload payload);


    record SubscriptionDto(
            Long id,
            Long clientId,
            Long offreId,
            String dateDebut,
            String dateFin,
            String status,
            String createdAt
    ) {}

    record QuotaDeductionPayload(
            String usageType,
            java.math.BigDecimal quantity
    ) {}

    record QuotaDeductionResult(
            boolean success,
            String message,
            String usageType,
            java.math.BigDecimal remaining,
            java.math.BigDecimal deducted,
            java.math.BigDecimal total,
            Long subscriptionId
    ) {}
}
