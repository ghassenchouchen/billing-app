package com.telecom.billing.infrastructure.kafka;

import com.telecom.billing.application.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventListener {

    private final BillingService billingService;

    @KafkaListener(
        topics = "subscription-events",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onSubscriptionEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            if (eventType == null) {
                log.warn("Ignoring subscription event without eventType");
                return;
            }

            switch (eventType) {
                case "ABONNEMENT_ACTIVATED" -> handleActivated(event);
                case "ABONNEMENT_CREATED" -> handleCreated(event);
                default -> log.debug("Ignoring subscription event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing subscription event: {}", e.getMessage(), e);
        }
    }

    private void handleCreated(Map<String, Object> event) {
        Long abonnementId = asLong(event.get("abonnementId"));
        if (abonnementId == null) {
            log.warn("Ignoring ABONNEMENT_CREATED event without abonnementId");
            return;
        }
        log.info("Received ABONNEMENT_CREATED event for subscription {}. " +
                 "Invoice will be generated upon activation or next billing cycle.", abonnementId);
    }

    private void handleActivated(Map<String, Object> event) {
        Long abonnementId = asLong(event.get("abonnementId"));
        if (abonnementId == null) {
            log.warn("Ignoring ABONNEMENT_ACTIVATED event without abonnementId");
            return;
        }

        String dateDebutStr = (String) event.get("dateDebut");
        LocalDate periodeDebut;
        try {
            periodeDebut = dateDebutStr != null ? LocalDate.parse(dateDebutStr) : LocalDate.now();
        } catch (Exception e) {
            periodeDebut = LocalDate.now();
        }

        LocalDate periodeFin = periodeDebut.plusMonths(1).minusDays(1);

        try {
            billingService.generateInvoiceForSubscription(abonnementId, periodeDebut, periodeFin);
            log.info("Generated initial invoice for activated subscription {}", abonnementId);
        } catch (Exception e) {
            log.warn("Failed to generate invoice for activated subscription {}: {}",
                    abonnementId, e.getMessage());
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
