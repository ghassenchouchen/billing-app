package com.telecom.boutique.infrastructure.kafka;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SimLifecycleEvent(
        String eventId,
        String eventType,
        String eventVersion,
        String occurredAt,
        String source,
        Map<String, Object> data
) {
    public static SimLifecycleEvent of(String eventType, Map<String, Object> data) {
        return new SimLifecycleEvent(
                UUID.randomUUID().toString(),
                eventType,
                "v1",
                Instant.now().toString(),
                "boutique-service",
                data
        );
    }
}
