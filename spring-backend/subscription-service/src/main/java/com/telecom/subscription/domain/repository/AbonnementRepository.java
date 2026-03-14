package com.telecom.subscription.domain.repository;

import com.telecom.subscription.domain.entity.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
    
    List<Abonnement> findByClientId(Long clientId);
    
    List<Abonnement> findByStatus(Abonnement.AbonnementStatus status);
    
    List<Abonnement> findByClientIdAndStatus(Long clientId, Abonnement.AbonnementStatus status);
    
    List<Abonnement> findByClientIdAndOffreIdAndStatus(Long clientId, Long offreId, Abonnement.AbonnementStatus status);
    
    List<Abonnement> findByClientRef(String clientRef);
}
