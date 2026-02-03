package com.telecom.usage.infrastructure.kafka;

import com.telecom.usage.domain.entity.UsageRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes usage events to Kafka with proper headers for tracking and tracing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsageEventPublisher {
    
    private static final String TOPIC_USAGE_RECORDED = "usage.events";
    private static final String EVENT_VERSION = "1.0";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Publish usage recorded event with correlation ID and event version headers
     */
    public void publishUsageRecorded(UsageRecord usage) {
        publishUsageRecorded(usage, null, EVENT_VERSION);
    }
    
    /**
     * Publish usage recorded event with custom correlation ID and event version
     */
    public void publishUsageRecorded(UsageRecord usage, String correlationId, String eventVersion) {
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        if (eventVersion == null) {
            eventVersion = EVENT_VERSION;
        }
        
        String eventId = UUID.randomUUID().toString();
        
        // Create event payload
        Map<String, Object> event = Map.of(
            "eventId", eventId,
            "eventType", "USAGE_RECORDED",
            "timestamp", Instant.now().toString(),
            "source", "usage-service",
            "payload", Map.of(
                "usageId", usage.getId(),
                "sessionId", usage.getSessionId(),
                "contratId", usage.getContratId(),
                "serviceId", usage.getServiceId(),
                "quantite", usage.getQuantite().doubleValue(),
                "dateUsage", usage.getDateUsage().toString(),
                "cdrSource", usage.getCdrSource(),
                "status", usage.getStatus().name(),
                "rated", usage.isRated()
            )
        );
        
        // Build message with headers for tracing
        Message<Map<String, Object>> message = MessageBuilder
            .withPayload(event)
            .setHeader("correlation-id", correlationId)
            .setHeader("event-version", eventVersion)
            .setHeader("event-id", eventId)
            .setHeader("source", "usage-service")
            .setHeader("event-type", "USAGE_RECORDED")
            .setHeader(KafkaHeaders.TOPIC, TOPIC_USAGE_RECORDED)
            .setHeader("kafka_messageKey", usage.getContratId().toString())
            .build();
        
        try {
            kafkaTemplate.send(message);
            log.info("Published USAGE_RECORDED event - usageId: {}, sessionId: {}, correlationId: {}, eventVersion: {}", 
                usage.getId(), usage.getSessionId(), correlationId, eventVersion);
        } catch (Exception e) {
            log.warn("Failed to publish USAGE_RECORDED event to Kafka - usageId: {}, sessionId: {}, reason: {}", 
                usage.getId(), usage.getSessionId(), e.getMessage());
        }
    }
    
    /**
     * Publish usage validation failed event
     */
    public void publishUsageValidationFailed(String sessionId, String reason, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        Map<String, Object> event = Map.of(
            "eventId", eventId,
            "eventType", "USAGE_VALIDATION_FAILED",
            "timestamp", Instant.now().toString(),
            "source", "usage-service",
            "payload", Map.of(
                "sessionId", sessionId,
                "reason", reason
            )
        );
        
        Message<Map<String, Object>> message = MessageBuilder
            .withPayload(event)
            .setHeader("correlation-id", correlationId)
            .setHeader("event-version", EVENT_VERSION)
            .setHeader("event-id", eventId)
            .setHeader("source", "usage-service")
            .setHeader("event-type", "USAGE_VALIDATION_FAILED")
            .setHeader(KafkaHeaders.TOPIC, TOPIC_USAGE_RECORDED)
            .build();
        
        try {
            kafkaTemplate.send(message);
            log.warn("Published USAGE_VALIDATION_FAILED event - sessionId: {}, reason: {}, correlationId: {}", 
                sessionId, reason, correlationId);
        } catch (Exception e) {
            log.warn("Failed to publish USAGE_VALIDATION_FAILED event to Kafka - sessionId: {}, reason: {}", 
                sessionId, reason);
        }
    }
    
    /**
     * Publish CDR batch processed event
     */
    public void publishCdrBatchProcessed(String sessionId, long recordCount, String cdrSource, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        Map<String, Object> event = Map.of(
            "eventId", eventId,
            "eventType", "CDR_BATCH_PROCESSED",
            "timestamp", Instant.now().toString(),
            "source", "usage-service",
            "payload", Map.of(
                "sessionId", sessionId,
                "recordCount", recordCount,
                "cdrSource", cdrSource
            )
        );
        
        Message<Map<String, Object>> message = MessageBuilder
            .withPayload(event)
            .setHeader("correlation-id", correlationId)
            .setHeader("event-version", EVENT_VERSION)
            .setHeader("event-id", eventId)
            .setHeader("source", "usage-service")
            .setHeader("event-type", "CDR_BATCH_PROCESSED")
            .setHeader(KafkaHeaders.TOPIC, TOPIC_USAGE_RECORDED)
            .build();
        
        try {
            kafkaTemplate.send(message);
            log.info("Published CDR_BATCH_PROCESSED event - sessionId: {}, recordCount: {}, cdrSource: {}", 
                sessionId, recordCount, cdrSource);
        } catch (Exception e) {
            log.warn("Failed to publish CDR_BATCH_PROCESSED event to Kafka - sessionId: {}, recordCount: {}", 
                sessionId, recordCount);
        }
    }
}
