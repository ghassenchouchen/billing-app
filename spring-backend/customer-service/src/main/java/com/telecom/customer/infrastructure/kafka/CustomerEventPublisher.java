package com.telecom.customer.infrastructure.kafka;

import com.telecom.customer.domain.entity.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventPublisher {
    
    private static final String TOPIC_CUSTOMER_CREATED = "billing.customer.created";
    private static final String TOPIC_CUSTOMER_UPDATED = "billing.customer.updated";
    private static final String TOPIC_CUSTOMER_SUSPENDED = "billing.customer.suspended";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishCustomerCreated(Client client) {
        Map<String, Object> event = buildEvent("billing.customer.created", client);
        kafkaTemplate.send(TOPIC_CUSTOMER_CREATED, client.getId().toString(), event);
        log.info("Published customer.created event for customer: {}", client.getId());
    }
    
    public void publishCustomerUpdated(Client client) {
        Map<String, Object> event = buildEvent("billing.customer.updated", client);
        kafkaTemplate.send(TOPIC_CUSTOMER_UPDATED, client.getId().toString(), event);
        log.info("Published customer.updated event for customer: {}", client.getId());
    }
    
    public void publishCustomerSuspended(Client client) {
        Map<String, Object> event = buildEvent("billing.customer.suspended", client);
        kafkaTemplate.send(TOPIC_CUSTOMER_SUSPENDED, client.getId().toString(), event);
        log.info("Published customer.suspended event for customer: {}", client.getId());
    }
    
    private Map<String, Object> buildEvent(String eventType, Client client) {
        return Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", eventType,
            "timestamp", Instant.now().toString(),
            "source", "customer-service",
            "payload", Map.of(
                "customerId", client.getId(),
                "email", client.getEmail(),
                "nom", client.getNom(),
                "prenom", client.getPrenom(),
                "type", client.getType().name(),
                "status", client.getStatus().name()
            )
        );
    }
}
