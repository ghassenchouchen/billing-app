package com.telecom.customer.infrastructure.kafka;

import com.telecom.customer.domain.entity.Customer;
import com.telecom.customer.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimLifecycleEventListener {

    private final CustomerRepository customerRepository;

    @KafkaListener(topics = "${kafka.topics.sim-lifecycle:boutique.sim.lifecycle}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSimLifecycleEvent(Map<String, Object> event) {
        Object dataObj = event.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            log.warn("Ignoring malformed sim lifecycle event: missing data payload");
            return;
        }

        Long clientId = asLong(dataMap.get("clientId"));
        if (clientId == null) {
            log.warn("Ignoring sim lifecycle event without clientId");
            return;
        }

        Optional<Customer> customerOpt = customerRepository.findById(clientId);
        if (customerOpt.isEmpty()) {
            log.warn("Ignoring sim lifecycle event for unknown customer id={}", clientId);
            return;
        }

        Customer customer = customerOpt.get();
        boolean nextHasSim = resolveHasSim(dataMap);
        if (Boolean.valueOf(nextHasSim).equals(customer.getHasSim())) {
            return;
        }

        customer.setHasSim(nextHasSim);
        customerRepository.save(customer);
        log.info("Updated customer {} hasSim={} from SIM lifecycle event", customer.getCustomerRef(), nextHasSim);
    }

    private boolean resolveHasSim(Map<?, ?> dataMap) {
        Long activeSimCount = asLong(dataMap.get("activeSimCount"));
        if (activeSimCount != null) {
            return activeSimCount > 0;
        }

        Object statusObj = dataMap.get("simStatus");
        return statusObj != null && ("ACTIVATED".equals(statusObj.toString()) || "ASSIGNED".equals(statusObj.toString()));
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
