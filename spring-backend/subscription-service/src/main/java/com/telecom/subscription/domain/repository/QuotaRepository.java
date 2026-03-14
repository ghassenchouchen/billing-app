package com.telecom.subscription.domain.repository;

import com.telecom.subscription.domain.entity.Quota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaRepository extends JpaRepository<Quota, Long> {

    List<Quota> findByAbonnementId(Long abonnementId);

    Optional<Quota> findByAbonnementIdAndQuotaType(Long abonnementId, Quota.QuotaType quotaType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quota q WHERE q.abonnement.id = :abonnementId AND q.quotaType = :quotaType")
    Optional<Quota> findByAbonnementIdAndQuotaTypeForUpdate(
            @Param("abonnementId") Long abonnementId,
            @Param("quotaType") Quota.QuotaType quotaType);
}
