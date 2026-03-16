package com.telecom.boutique.domain.repository;

import com.telecom.boutique.domain.entity.StockSim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockSimRepository extends JpaRepository<StockSim, Long> {

    List<StockSim> findByBoutiqueId(Long boutiqueId);

    Optional<StockSim> findByIccid(String iccid);

    List<StockSim> findByBoutiqueIdAndStatus(Long boutiqueId, StockSim.SimStatus status);

    @Query("SELECT s.simType, s.status, COUNT(s) FROM StockSim s WHERE s.boutiqueId = :boutiqueId GROUP BY s.simType, s.status")
    List<Object[]> countByBoutiqueGrouped(Long boutiqueId);

    long countByBoutiqueIdAndStatus(Long boutiqueId, StockSim.SimStatus status);

    long countByAssignedToClientIdAndStatus(Long assignedToClientId, StockSim.SimStatus status);

    @Query("SELECT s.iccid FROM StockSim s WHERE s.iccid IN :iccids")
    List<String> findIccidsIn(@Param("iccids") List<String> iccids);
}
