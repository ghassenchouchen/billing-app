package com.telecom.subscription.infrastructure.kafka;

import com.telecom.subscription.domain.entity.Abonnement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "subscription-events";


    public void publishAbonnementCreated(Abonnement abonnement) {
        Map<String, Object> event = buildEvent("ABONNEMENT_CREATED", abonnement);
        try {
            kafkaTemplate.send(TOPIC, abonnement.getId().toString(), event);
            log.info("Published ABONNEMENT_CREATED event for subscription {}", abonnement.getId());
        } catch (Exception ex) {
            log.warn("Failed to publish ABONNEMENT_CREATED event: {}", ex.getMessage());
        }
    }

    public void publishAbonnementActivated(Abonnement abonnement) {
        Map<String, Object> event = buildEvent("ABONNEMENT_ACTIVATED", abonnement);
        try {
            kafkaTemplate.send(TOPIC, abonnement.getId().toString(), event);
            log.info("Published ABONNEMENT_ACTIVATED event for subscription {}", abonnement.getId());
        } catch (Exception ex) {
            log.warn("Failed to publish ABONNEMENT_ACTIVATED event: {}", ex.getMessage());
        }
    }

    public void publishAbonnementSuspended(Abonnement abonnement) {
        Map<String, Object> event = buildEvent("ABONNEMENT_SUSPENDED", abonnement);
        try {
            kafkaTemplate.send(TOPIC, abonnement.getId().toString(), event);
            log.info("Published ABONNEMENT_SUSPENDED event for subscription {}", abonnement.getId());
        } catch (Exception ex) {
            log.warn("Failed to publish ABONNEMENT_SUSPENDED event: {}", ex.getMessage());
        }
    }

    public void publishAbonnementTerminated(Abonnement abonnement) {
        Map<String, Object> event = buildEvent("ABONNEMENT_TERMINATED", abonnement);
        try {
            kafkaTemplate.send(TOPIC, abonnement.getId().toString(), event);
            log.info("Published ABONNEMENT_TERMINATED event for subscription {}", abonnement.getId());
        } catch (Exception ex) {
            log.warn("Failed to publish ABONNEMENT_TERMINATED event: {}", ex.getMessage());
        }
    }

    private Map<String, Object> buildEvent(String eventType, Abonnement abonnement) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("abonnementId", abonnement.getId());
        event.put("clientId", abonnement.getClientId());
        event.put("offreId", abonnement.getOffreId());
        event.put("status", abonnement.getStatus().name());
        event.put("dateDebut", abonnement.getDateDebut() != null ? abonnement.getDateDebut().toString() : null);
        event.put("dateFin", abonnement.getDateFin() != null ? abonnement.getDateFin().toString() : null);
        return event;
    }
}
