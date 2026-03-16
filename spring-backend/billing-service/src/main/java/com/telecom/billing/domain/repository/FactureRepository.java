package com.telecom.billing.domain.repository;

import com.telecom.billing.domain.entity.Facture;
import com.telecom.billing.domain.entity.Facture.FactureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    
    Optional<Facture> findByNumeroFacture(String numeroFacture);
    
    List<Facture> findByClientId(Long clientId);
    
    List<Facture> findByClientIdAndStatut(Long clientId, FactureStatus statut);
    
    List<Facture> findByAbonnementId(Long abonnementId);
    
    List<Facture> findByStatut(FactureStatus statut);
    
    @Query("SELECT f FROM Facture f WHERE f.statut = :statut AND f.dateEcheance < :today")
    List<Facture> findOverdueInvoices(
        @Param("statut") FactureStatus statut,
        @Param("today") LocalDate today
    );
    
    @Query("SELECT f FROM Facture f WHERE f.dateFacture BETWEEN :startDate AND :endDate")
    List<Facture> findByPeriod(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT f FROM Facture f WHERE f.clientId = :clientId " +
           "AND f.periodeDebut = :periodeDebut AND f.periodeFin = :periodeFin")
    Optional<Facture> findByClientIdAndPeriod(
        @Param("clientId") Long clientId,
        @Param("periodeDebut") LocalDate periodeDebut,
        @Param("periodeFin") LocalDate periodeFin
    );
    
    @Query("SELECT COALESCE(SUM(f.montantTTC), 0) FROM Facture f " +
           "WHERE f.clientId = :clientId AND f.statut IN ('SENT', 'OVERDUE')")
    java.math.BigDecimal calculateOutstandingBalance(@Param("clientId") Long clientId);
    
    boolean existsByNumeroFacture(String numeroFacture);
}
