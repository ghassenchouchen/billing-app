package com.telecom.billing.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service", url = "${services.subscription-url}")
public interface SubscriptionClient {
    
    @GetMapping("/subscriptions/{id}")
    AbonnementDto getAbonnement(@PathVariable("id") Long id);
    
    @GetMapping("/subscriptions/client/{clientId}/active")
    java.util.List<AbonnementDto> getActiveAbonnementsByClient(@PathVariable("clientId") Long clientId);
    
    @GetMapping("/subscriptions/active")
    java.util.List<AbonnementDto> getAllActiveAbonnements();
    
    record AbonnementDto(
        Long id,
        Long clientId,
        Long offreId,
        String dateDebut,
        String dateFin,
        String status,
        String billingFrequency,
        String lastBillingDate,
        String nextBillingDate
    ) {}
}
