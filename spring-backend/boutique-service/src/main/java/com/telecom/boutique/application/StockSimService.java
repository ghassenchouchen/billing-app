package com.telecom.boutique.application;

import com.telecom.boutique.domain.entity.StockSim;
import com.telecom.boutique.domain.exception.BoutiqueNotFoundException;
import com.telecom.boutique.domain.exception.CustomerNotFoundException;
import com.telecom.boutique.domain.exception.DuplicateResourceException;
import com.telecom.boutique.domain.exception.SimNotAvailableException;
import com.telecom.boutique.domain.exception.SimNotFoundException;
import com.telecom.boutique.domain.repository.BoutiqueRepository;
import com.telecom.boutique.domain.repository.StockSimRepository;
import com.telecom.boutique.infrastructure.client.CustomerClient;
import com.telecom.boutique.infrastructure.kafka.SimEventPublisher;
import com.telecom.boutique.web.dto.AddSimRequest;
import com.telecom.boutique.web.dto.StockSimDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSimService {

    private final StockSimRepository stockSimRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final SimEventPublisher simEventPublisher;
    private final CustomerClient customerClient;

    @Transactional(readOnly = true)
    public List<StockSimDto> getStockByBoutique(Long boutiqueId) {
        return stockSimRepository.findByBoutiqueId(boutiqueId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StockSimDto> getAvailableStock(Long boutiqueId) {
        return stockSimRepository.findByBoutiqueIdAndStatus(boutiqueId, StockSim.SimStatus.AVAILABLE)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public StockSimDto assignSim(String iccid, Long clientId) {
        validateCustomerOrThrow(clientId);
        StockSim sim = findByIccidOrThrow(iccid);
        if (sim.getStatus() != StockSim.SimStatus.AVAILABLE) {
            throw new SimNotAvailableException(iccid, sim.getStatus().name());
        }
        sim.setStatus(StockSim.SimStatus.ACTIVATED);
        sim.setAssignedToClientId(clientId);
        sim.setAssignedAt(LocalDateTime.now());
        sim = stockSimRepository.save(sim);
        long activeSimCount = stockSimRepository.countByAssignedToClientIdAndStatus(
            clientId, StockSim.SimStatus.ACTIVATED);
        simEventPublisher.publishActivated(sim, activeSimCount);
        log.info("Assigned and activated SIM {} for client {}", iccid, clientId);
        return toDto(sim);
    }

    @Transactional
    public StockSimDto activateSim(String iccid, Long clientId) {
        validateCustomerOrThrow(clientId);
        StockSim sim = findByIccidOrThrow(iccid);
        if (sim.getStatus() != StockSim.SimStatus.AVAILABLE && sim.getStatus() != StockSim.SimStatus.ASSIGNED) {
            throw new SimNotAvailableException(iccid, sim.getStatus().name());
        }
        sim.setStatus(StockSim.SimStatus.ACTIVATED);
        sim.setAssignedToClientId(clientId);
        sim.setAssignedAt(LocalDateTime.now());
        sim = stockSimRepository.save(sim);
        long activeSimCount = stockSimRepository.countByAssignedToClientIdAndStatus(
            clientId, StockSim.SimStatus.ACTIVATED);
        simEventPublisher.publishActivated(sim, activeSimCount);
        log.info("Activated SIM {} for client {}", iccid, clientId);
        return toDto(sim);
    }

    @Transactional
    public StockSimDto suspendSim(String iccid) {
        StockSim sim = findByIccidOrThrow(iccid);
        if (sim.getStatus() != StockSim.SimStatus.ACTIVATED) {
            throw new SimNotAvailableException(iccid, sim.getStatus().name());
        }
        sim.setStatus(StockSim.SimStatus.SUSPENDED);
        sim = stockSimRepository.save(sim);
        long activeSimCount = stockSimRepository.countByAssignedToClientIdAndStatus(
                sim.getAssignedToClientId(), StockSim.SimStatus.ACTIVATED);
        simEventPublisher.publishSuspended(sim, activeSimCount);
        log.info("Suspended SIM {} for client {}", iccid, sim.getAssignedToClientId());
        return toDto(sim);
    }

    @Transactional
    public StockSimDto deactivateSim(String iccid) {
        StockSim sim = findByIccidOrThrow(iccid);
        if (sim.getStatus() != StockSim.SimStatus.ACTIVATED && sim.getStatus() != StockSim.SimStatus.SUSPENDED) {
            throw new SimNotAvailableException(iccid, sim.getStatus().name());
        }
        sim.setStatus(StockSim.SimStatus.DEACTIVATED);
        sim = stockSimRepository.save(sim);
        long activeSimCount = stockSimRepository.countByAssignedToClientIdAndStatus(
                sim.getAssignedToClientId(), StockSim.SimStatus.ACTIVATED);
        simEventPublisher.publishDeactivated(sim, activeSimCount);
        log.info("Deactivated SIM {} for client {}", iccid, sim.getAssignedToClientId());
        return toDto(sim);
    }

    @Transactional
    public List<StockSimDto> addSimBatch(Long boutiqueId, List<AddSimRequest> sims) {
        if (!boutiqueRepository.existsById(boutiqueId)) {
            throw new BoutiqueNotFoundException(boutiqueId);
        }
        List<String> incomingIccids = sims.stream().map(AddSimRequest::iccid).toList();
        List<String> existing = stockSimRepository.findIccidsIn(incomingIccids);
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException("ICCIDs already in stock: " + existing);
        }
        List<StockSim> entities = sims.stream().map(req -> StockSim.builder()
                .iccid(req.iccid())
                .imsi(req.imsi())
                .msisdn(req.msisdn())
                .simType(StockSim.SimType.valueOf(req.simType()))
                .status(StockSim.SimStatus.AVAILABLE)
                .boutiqueId(boutiqueId)
                .build()
        ).toList();
        List<StockSim> saved = stockSimRepository.saveAll(entities);
        log.info("Batch added {} SIMs to boutique {}", saved.size(), boutiqueId);
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countAvailable(Long boutiqueId) {
        return stockSimRepository.countByBoutiqueIdAndStatus(boutiqueId, StockSim.SimStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<Object[]> countGrouped(Long boutiqueId) {
        return stockSimRepository.countByBoutiqueGrouped(boutiqueId);
    }

    private StockSim findByIccidOrThrow(String iccid) {
        return stockSimRepository.findByIccid(iccid)
                .orElseThrow(() -> new SimNotFoundException(iccid));
    }

    private void validateCustomerOrThrow(Long customerId) {
        if (customerId == null || !customerClient.customerExists(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }
    }

    private StockSimDto toDto(StockSim s) {
        return new StockSimDto(s.getId(), s.getIccid(), s.getImsi(), s.getMsisdn(),
                s.getSimType().name(), s.getStatus().name(), s.getBoutiqueId(),
                s.getAssignedToClientId(), s.getAssignedAt(), s.getCreatedAt());
    }
}
