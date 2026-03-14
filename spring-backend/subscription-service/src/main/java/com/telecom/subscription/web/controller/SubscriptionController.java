package com.telecom.subscription.web.controller;

import com.telecom.subscription.application.QuotaService;
import com.telecom.subscription.application.SubscriptionService;
import com.telecom.subscription.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final QuotaService quotaService;


    @GetMapping("/subscriptions")
    public ResponseEntity<List<AbonnementDto>> getAllAbonnements() {
        log.debug("GET /subscriptions - fetching all abonnements");
        return ResponseEntity.ok(subscriptionService.getAllAbonnements());
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<AbonnementDto> getAbonnement(@PathVariable Long id) {
        log.debug("GET /subscriptions/{} - fetching abonnement by id", id);
        return ResponseEntity.ok(subscriptionService.getAbonnementById(id));
    }

    @GetMapping("/subscriptions/client/{clientId}")
    public ResponseEntity<List<AbonnementDto>> getAbonnementsByClient(@PathVariable Long clientId) {
        log.debug("GET /subscriptions/client/{} - fetching abonnements by client id", clientId);
        return ResponseEntity.ok(subscriptionService.getAbonnementsByClientId(clientId));
    }


    @GetMapping("/subscriptions/client/ref/{customerRef}")
    public ResponseEntity<List<AbonnementDto>> getAbonnementsByClientRef(@PathVariable String customerRef) {
        log.info("GET /subscriptions/client/ref/{} - fetching abonnements by customer ref", customerRef);
        try {
            List<AbonnementDto> abonnements = subscriptionService.getAbonnementsByClientRef(customerRef);
            log.info("Successfully retrieved {} abonnements for customer ref: {}", abonnements.size(), customerRef);
            return ResponseEntity.ok(abonnements);
        } catch (Exception ex) {
            log.error("Error retrieving abonnements for customer ref: {}", customerRef, ex);
            throw ex;
        }
    }
    @GetMapping("/subscriptions/active")
    public ResponseEntity<List<AbonnementDto>> getActiveAbonnements() {
        return ResponseEntity.ok(subscriptionService.getActiveAbonnements());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<AbonnementDto> createAbonnement(@RequestBody CreateAbonnementRequest request) {
        return ResponseEntity.ok(subscriptionService.createAbonnement(request));
    }

    @PutMapping("/subscriptions/{id}/activate")
    public ResponseEntity<AbonnementDto> activateAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.activateAbonnement(id));
    }

    @PutMapping("/subscriptions/{id}/suspend")
    public ResponseEntity<AbonnementDto> suspendAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.suspendAbonnement(id));
    }

    @PutMapping("/subscriptions/{id}/terminate")
    public ResponseEntity<AbonnementDto> terminateAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.terminateAbonnement(id));
    }

    // ── Quota endpoints ───────────────────────────────────────────────

    @GetMapping("/subscriptions/{id}/quotas")
    public ResponseEntity<List<QuotaDto>> getQuotas(@PathVariable Long id) {
        return ResponseEntity.ok(quotaService.getQuotasBySubscription(id));
    }

    @PostMapping("/subscriptions/{id}/quotas/initialize")
    public ResponseEntity<List<QuotaDto>> initializeQuotas(
            @PathVariable Long id,
            @RequestBody List<QuotaService.QuotaInitRequest> quotas) {
        return ResponseEntity.ok(quotaService.initializeQuotas(id, quotas));
    }

    @PostMapping("/subscriptions/{id}/quotas/deduct")
    public ResponseEntity<QuotaDeductionResponse> deductQuota(
            @PathVariable Long id,
            @RequestBody QuotaDeductionRequest request) {
        return ResponseEntity.ok(quotaService.deductQuota(id, request));
    }

    @PostMapping("/subscriptions/{id}/quotas/reset")
    public ResponseEntity<List<QuotaDto>> resetQuotas(@PathVariable Long id) {
        return ResponseEntity.ok(quotaService.resetQuotas(id));
    }
}
