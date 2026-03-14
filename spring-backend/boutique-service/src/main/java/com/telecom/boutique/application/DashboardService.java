package com.telecom.boutique.application;

import com.telecom.boutique.domain.entity.StockSim;
import com.telecom.boutique.domain.repository.TransactionRepository;
import com.telecom.boutique.web.dto.DashboardDto;
import com.telecom.boutique.web.dto.TransactionDto;
import com.telecom.boutique.domain.entity.TransactionBoutique;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final StockSimService stockSimService;

    @Value("${boutique.dashboard.contract-target:200}")
    private long contractTarget;

    @Value("${boutique.dashboard.low-stock-threshold:10}")
    private long lowStockThreshold;


    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactions(Long boutiqueId) {
        return transactionRepository.findByBoutiqueIdOrderByCreatedAtDesc(boutiqueId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTodayTransactions(Long boutiqueId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return transactionRepository
                .findByBoutiqueIdAndCreatedAtBetweenOrderByCreatedAtDesc(boutiqueId, start, end)
                .stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Long boutiqueId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueToday = transactionRepository
                .sumRevenueByBoutique(boutiqueId, todayStart, todayEnd);
        long contractsMonth = transactionRepository
                .countByBoutiqueIdAndCreatedAtBetween(boutiqueId, monthStart, todayEnd);
        long simAvailable = stockSimService.countAvailable(boutiqueId);

        Map<String, Long> simByType = new HashMap<>();
        List<Object[]> grouped = stockSimService.countGrouped(boutiqueId);
        for (Object[] row : grouped) {
            String type = ((StockSim.SimType) row[0]).name();
            StockSim.SimStatus st = (StockSim.SimStatus) row[1];
            long count = (Long) row[2];
            if (st == StockSim.SimStatus.AVAILABLE) {
                simByType.merge(type, count, Long::sum);
            }
        }

        long lowStock = simByType.values().stream()
                .filter(c -> c < lowStockThreshold).count();

        return new DashboardDto(revenueToday, contractsMonth, contractTarget,
                simAvailable, lowStock, simByType);
    }

    private TransactionDto toDto(TransactionBoutique t) {
        return new TransactionDto(t.getId(), t.getReference(), t.getBoutiqueId(), t.getAgentId(),
                t.getClientId(), t.getClientNom(), t.getOffreLibelle(),
                t.getTypeTransaction().name(), t.getMontant(), t.getStatus().name(), t.getCreatedAt());
    }
}
