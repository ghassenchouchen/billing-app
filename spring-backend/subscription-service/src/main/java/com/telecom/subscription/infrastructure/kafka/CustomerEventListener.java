package com.telecom.subscription.infrastructure.kafka;

import com.telecom.subscription.domain.entity.Abonnement;
import com.telecom.subscription.domain.repository.AbonnementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/*
  Listens to customer lifecycle events from the customer-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventListener {

    private final AbonnementRepository abonnementRepository;

    @KafkaListener(
        topics = "billing.customer.suspended",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void onCustomerSuspended(Map<String, Object> event) {
        try {
            Object dataObj = event.get("data");
          
            Map<?, ?> data = (Map<?, ?>) dataObj;
            String customerRef = (String) data.get("customerRef");
            if (customerRef == null || customerRef.isBlank()) {
                log.warn("Ignoring customer.suspended event without customerRef");
                return;
            }

            List<Abonnement> activeSubscriptions = abonnementRepository
                .findByClientRef(customerRef)
                .stream()
                .filter(a -> a.getStatus() == Abonnement.AbonnementStatus.ACTIVE)
                .toList();

            if (activeSubscriptions.isEmpty()) {
                log.info("No active subscriptions found for suspended customer {}", customerRef);
                return;
            }

            int suspended = 0;
            for (Abonnement abonnement : activeSubscriptions) {
                abonnement.suspend();
                abonnementRepository.save(abonnement);
                suspended++;
                log.info("Suspended subscription {} for customer {}", abonnement.getId(), customerRef);
            }

            log.info("Suspended {} active subscriptions for customer {}", suspended, customerRef);

        } catch (Exception e) {
            log.error("Error processing customer.suspended event: {}", e.getMessage(), e);
        }
    }
}
