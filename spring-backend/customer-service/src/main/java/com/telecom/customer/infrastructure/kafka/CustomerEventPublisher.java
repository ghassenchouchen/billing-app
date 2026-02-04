package com.telecom.customer.infrastructure.kafka;

import com.telecom.customer.domain.entity.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/* 
 * consumers:
 * - subscription-service (validates customer exists before creating subscription)
 * - billing-service (creates billing account, links customer to invoices)
 * - later :  notification-service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventPublisher {
    
    private static final String TOPIC_CUSTOMER_CREATED = "billing.customer.created";
    private static final String TOPIC_CUSTOMER_UPDATED = "billing.customer.updated";
    private static final String TOPIC_CUSTOMER_SUSPENDED = "billing.customer.suspended";
    
    private static final String SCHEMA_VERSION = "1.0.0";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * KEY: customerRef (ensures all events for same customer go to same partition)
     */
    public void publishCustomerCreated(Customer customer) {
        Map<String, Object> event = buildCustomerCreatedEvent(customer);
        
        kafkaTemplate.send(TOPIC_CUSTOMER_CREATED, customer.getCustomerRef(), event);
        
        log.info("Published customer.created event for customerRef: {} (id: {})", 
            customer.getCustomerRef(), customer.getId());
    }
    
    /**
     * Publish customer update event
     */
    public void publishCustomerUpdated(Customer customer) {
        Map<String, Object> event = buildCustomerUpdatedEvent(customer);
        
        kafkaTemplate.send(TOPIC_CUSTOMER_UPDATED, customer.getCustomerRef(), event);
        
        log.info("Published customer.updated event for customerRef: {} (id: {})", 
            customer.getCustomerRef(), customer.getId());
    }
    
    /**
     * Publish customer suspension event
     * CRITICAL: Downstream services must react immediately
     */
    public void publishCustomerSuspended(Customer customer) {
        Map<String, Object> event = buildCustomerSuspendedEvent(customer);
        
        kafkaTemplate.send(TOPIC_CUSTOMER_SUSPENDED, customer.getCustomerRef(), event);
        
        log.warn("Published customer.suspended event for customerRef: {} (id: {})", 
            customer.getCustomerRef(), customer.getId());
    }
    
    /**
     * Build customer.created event with complete customer profile
     */
    private Map<String, Object> buildCustomerCreatedEvent(Customer customer) {
        Map<String, Object> event = new HashMap<>();
        
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "billing.customer.created");
        event.put("eventVersion", "v1");
        event.put("schemaVersion", SCHEMA_VERSION);
        event.put("timestamp", Instant.now().toString());
        event.put("source", "customer-service");
        
        Map<String, Object> data = new HashMap<>();
        
        data.put("customerRef", customer.getCustomerRef());          // PRIMARY and ONLY identifier
        data.put("email", customer.getEmail());                      // Alternative lookup
        
        
        data.put("nom", customer.getNom());
        data.put("prenom", customer.getPrenom());
        data.put("telephone", customer.getTelephone());
        
        data.put("adresse", customer.getAdresse());
        data.put("ville", customer.getVille());
        data.put("codePostal", customer.getCodePostal());
        data.put("pays", customer.getPays());
        
        data.put("type", customer.getType().name());
        data.put("status", customer.getStatus().name());
        data.put("paymentType", customer.getPaymentType().name());
        data.put("accountBalance", customer.getAccountBalance());
        data.put("creditLimit", customer.getCreditLimit());
        data.put("createdAt", customer.getCreatedAt().toString());
        
        event.put("data", data);
        
        return event;
    }
    
  
    private Map<String, Object> buildCustomerUpdatedEvent(Customer customer) {
        Map<String, Object> event = new HashMap<>();
        
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "billing.customer.updated");
        event.put("eventVersion", "v1");
        event.put("schemaVersion", SCHEMA_VERSION);
        event.put("timestamp", Instant.now().toString());
        event.put("source", "customer-service");
        
        Map<String, Object> data = new HashMap<>();
        
        data.put("customerRef", customer.getCustomerRef());
        data.put("email", customer.getEmail());
        
        data.put("nom", customer.getNom());
        data.put("prenom", customer.getPrenom());
        data.put("telephone", customer.getTelephone());
        data.put("adresse", customer.getAdresse());
        data.put("ville", customer.getVille());
        data.put("codePostal", customer.getCodePostal());
        data.put("pays", customer.getPays());
        
        data.put("updatedAt", customer.getUpdatedAt() != null ? 
            customer.getUpdatedAt().toString() : Instant.now().toString());
        
        event.put("data", data);
        
        return event;
    }
    
 
    private Map<String, Object> buildCustomerSuspendedEvent(Customer customer) {
        Map<String, Object> event = new HashMap<>();
        
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "billing.customer.suspended");
        event.put("eventVersion", "v1");
        event.put("schemaVersion", SCHEMA_VERSION);
        event.put("timestamp", Instant.now().toString());
        event.put("source", "customer-service");
        
        Map<String, Object> data = new HashMap<>();
        
        data.put("customerRef", customer.getCustomerRef());
        data.put("email", customer.getEmail());
        
        data.put("previousStatus", "ACTIVE");  // Could track in entity
        data.put("newStatus", customer.getStatus().name());
        data.put("suspendedAt", Instant.now().toString());
        
        // Actions for downstream services
        Map<String, Boolean> requiredActions = new HashMap<>();
        requiredActions.put("blockNewSubscriptions", true);
        requiredActions.put("suspendActiveSubscriptions", true);
        requiredActions.put("stopBillingCycles", true);
        requiredActions.put("preventNewCharges", true);
        requiredActions.put("sendNotification", true);
        data.put("requiredActions", requiredActions);
        
        event.put("data", data);
        
        return event;
    }
}
