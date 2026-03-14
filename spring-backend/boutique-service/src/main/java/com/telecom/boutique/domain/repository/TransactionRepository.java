package com.telecom.boutique.domain.repository;

import com.telecom.boutique.domain.entity.TransactionBoutique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionBoutique, Long> {

    List<TransactionBoutique> findByBoutiqueIdOrderByCreatedAtDesc(Long boutiqueId);

    List<TransactionBoutique> findByBoutiqueIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long boutiqueId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM TransactionBoutique t " +
           "WHERE t.boutiqueId = :boutiqueId AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenueByBoutique(Long boutiqueId, LocalDateTime from, LocalDateTime to);

    long countByBoutiqueIdAndCreatedAtBetween(Long boutiqueId, LocalDateTime from, LocalDateTime to);
}
