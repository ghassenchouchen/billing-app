package com.telecom.boutique.infrastructure.kafka;

import com.telecom.boutique.domain.entity.StockSim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.sim-lifecycle:boutique.sim.lifecycle}")
    private String simLifecycleTopic;

    public void publishAssigned(StockSim sim, long activeSimCount) {
        publish(sim, activeSimCount, "boutique.sim.assigned");
    }

    public void publishActivated(StockSim sim, long activeSimCount) {
        publish(sim, activeSimCount, "boutique.sim.activated");
    }

    public void publishSuspended(StockSim sim, long activeSimCount) {
        publish(sim, activeSimCount, "boutique.sim.suspended");
    }

    public void publishDeactivated(StockSim sim, long activeSimCount) {
        publish(sim, activeSimCount, "boutique.sim.deactivated");
    }

    private void publish(StockSim sim, long activeSimCount, String eventType) {
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", sim.getAssignedToClientId());
        data.put("iccid", sim.getIccid());
        data.put("simStatus", sim.getStatus().name());
        data.put("activeSimCount", activeSimCount);

        SimLifecycleEvent event = SimLifecycleEvent.of(eventType, data);
        String key = sim.getAssignedToClientId() != null ? String.valueOf(sim.getAssignedToClientId()) : sim.getIccid();

        try {
            kafkaTemplate.send(simLifecycleTopic, key, event);
            log.info("Published {} event for clientId={} iccid={} activeSimCount={}",
                    event.eventType(), sim.getAssignedToClientId(), sim.getIccid(), activeSimCount);
        } catch (Exception e) {
            log.warn("Failed to publish SIM lifecycle event (Kafka unavailable): {}", e.getMessage());
        }
    }
}
