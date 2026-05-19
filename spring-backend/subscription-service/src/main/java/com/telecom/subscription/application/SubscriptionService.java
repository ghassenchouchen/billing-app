package com.telecom.subscription.application;

import com.telecom.subscription.domain.entity.Abonnement;
import com.telecom.subscription.domain.entity.BillingFrequency;
import feign.FeignException;
import com.telecom.subscription.domain.repository.AbonnementRepository;
import com.telecom.subscription.infrastructure.client.CustomerClient;
import com.telecom.subscription.infrastructure.client.CatalogClient;
import com.telecom.subscription.infrastructure.kafka.SubscriptionEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import com.telecom.subscription.web.dto.AbonnementDto;
import com.telecom.subscription.web.dto.CreateAbonnementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final AbonnementRepository abonnementRepository;
    private final CustomerClient customerClient;
    private final CatalogClient catalogClient;
    private final ObjectProvider<SubscriptionEventPublisher> eventPublisherProvider;
    
    @Transactional(readOnly = true)
    public List<AbonnementDto> getAllAbonnements() {
        return abonnementRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public AbonnementDto getAbonnementById(Long id) {
        return abonnementRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new RuntimeException("Abonnement not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<AbonnementDto> getAbonnementsByClientId(Long clientId) {
        return abonnementRepository.findByClientId(clientId).stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<AbonnementDto> getActiveAbonnements() {
        return abonnementRepository.findByStatus(Abonnement.AbonnementStatus.ACTIVE).stream()
            .filter(Abonnement::isActive)
            .map(this::toDto)
            .toList();
    }
    
    @Transactional
    public AbonnementDto createAbonnement(CreateAbonnementRequest request) {
            catalogClient.getOffreById(request.offreId());
        
        Long resolvedClientId = request.clientId();
        boolean customerValidated = false;

        if (resolvedClientId != null) {
            try {
                customerClient.getCustomerById(resolvedClientId);
                customerValidated = true;
            } catch (FeignException ex) {
                log.warn("Customer lookup by id failed: {}", ex.getMessage());
                resolvedClientId = null;
            }
        }

        if (!customerValidated && request.clientRef() != null && !request.clientRef().isBlank()) {
            customerClient.getCustomerByRef(request.clientRef());
            customerValidated = true;
        }

        if (resolvedClientId == null && request.clientRef() != null && !request.clientRef().isBlank()) {
            resolvedClientId = deriveClientIdFromRef(request.clientRef());
        }

        if (resolvedClientId == null) {
            log.error("Client resolution failed for request: id={}, ref={}", request.clientId(), request.clientRef());
            throw new RuntimeException("Cannot create subscription: Client must be explicitly resolved. Invalid clientId or clientRef provided.");
        }

        if (!customerValidated) {
            log.warn("Customer not validated (id={}, ref={}); proceeding with clientRef only", 
                request.clientId(), request.clientRef());
        }
        
        // Check for duplicate active subscription for this offer
        List<Abonnement> existingActive = abonnementRepository.findByClientIdAndOffreIdAndStatus(
            resolvedClientId, 
            request.offreId(), 
            Abonnement.AbonnementStatus.ACTIVE
        );
        
        if (!existingActive.isEmpty()) {
            throw new RuntimeException(
                String.format("Customer already has an active subscription for offer ID %d", request.offreId())
            );
        }

        BillingFrequency frequency = request.billingFrequency() != null
            ? request.billingFrequency()
            : BillingFrequency.MONTHLY;
        
        Abonnement abonnement = Abonnement.builder()
            .clientId(resolvedClientId)
            .clientRef(request.clientRef())
            .offreId(request.offreId())
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .billingFrequency(frequency)
            .status(Abonnement.AbonnementStatus.ACTIVE)
            .build();
        
        abonnement = abonnementRepository.save(abonnement);
        log.info("Created abonnement: {}", abonnement.getId());
        
        SubscriptionEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAbonnementCreated(abonnement);
            } catch (Exception ex) {
                log.warn("Failed to publish abonnement created event: {}", ex.getMessage());
            }
        }
        
        return toDto(abonnement);
    }
    
    @Transactional
    public AbonnementDto activateAbonnement(Long id) {
        Abonnement abonnement = abonnementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Abonnement not found: " + id));
        
        abonnement.activate();
        abonnement = abonnementRepository.save(abonnement);
        
        SubscriptionEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAbonnementActivated(abonnement);
            } catch (Exception ex) {
                log.warn("Failed to publish abonnement activated event: {}", ex.getMessage());
            }
        }
        
        return toDto(abonnement);
    }
    
    @Transactional
    public AbonnementDto suspendAbonnement(Long id) {
        Abonnement abonnement = abonnementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Abonnement not found: " + id));
        
        abonnement.suspend();
        abonnement = abonnementRepository.save(abonnement);
        
        SubscriptionEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAbonnementSuspended(abonnement);
            } catch (Exception ex) {
                log.warn("Failed to publish abonnement suspended event: {}", ex.getMessage());
            }
        }
        
        return toDto(abonnement);
    }
    
    @Transactional
    public AbonnementDto terminateAbonnement(Long id) {
        Abonnement abonnement = abonnementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Abonnement not found: " + id));
        
        abonnement.terminate();
        abonnement = abonnementRepository.save(abonnement);
        
        SubscriptionEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAbonnementTerminated(abonnement);
            } catch (Exception ex) {
                log.warn("Failed to publish abonnement terminated event: {}", ex.getMessage());
            }
        }
        
        return toDto(abonnement);
    }
    
    private AbonnementDto toDto(Abonnement abonnement) {
        return new AbonnementDto(
            abonnement.getId(),
            abonnement.getClientId(),
            abonnement.getClientRef(),
            abonnement.getOffreId(),
            abonnement.getDateDebut(),
            abonnement.getDateFin(),
            abonnement.getStatus().name(),
            abonnement.getBillingFrequency() != null ? abonnement.getBillingFrequency().name() : null,
            abonnement.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AbonnementDto> getAbonnementsByClientRef(String clientRef) {
        if (clientRef == null || clientRef.isBlank()) {
            log.warn("Invalid clientRef provided: {}", clientRef);
            return List.of();
        }
        
        try {
            List<AbonnementDto> result = abonnementRepository.findByClientRef(clientRef).stream()
                .map(this::toDto)
                .toList();
            log.info("Found {} abonnements for clientRef: {}", result.size(), clientRef);
            return result;
        } catch (Exception ex) {
            log.error("Error fetching abonnements for clientRef: {}", clientRef, ex);
            throw new RuntimeException("Failed to fetch abonnements for customer ref: " + clientRef, ex);
        }
    }

    private Long deriveClientIdFromRef(String clientRef) {
        if (clientRef == null) return null;
        String digits = clientRef.replaceAll("\\D+", "");
        if (digits.isBlank()) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
