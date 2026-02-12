package com.telecom.usage.domain.repository;

import com.telecom.usage.domain.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    
    List<UsageRecord> findByAbonnementId(Long abonnementId);
    
    List<UsageRecord> findByAbonnementIdAndRatedFalse(Long abonnementId);
    
    Optional<UsageRecord> findBySessionId(String sessionId);
    
    boolean existsBySessionId(String sessionId);
    
    @Query("SELECT u FROM UsageRecord u WHERE u.abonnementId = :abonnementId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate")
    List<UsageRecord> findByAbonnementIdAndPeriod(
        @Param("abonnementId") Long abonnementId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT u FROM UsageRecord u WHERE u.abonnementId = :abonnementId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate AND u.rated = true")
    List<UsageRecord> findRatedByAbonnementIdAndPeriod(
        @Param("abonnementId") Long abonnementId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    List<UsageRecord> findBySubscriptionId(Long subscriptionId);
    
    @Query("SELECT u FROM UsageRecord u WHERE u.subscriptionId = :subscriptionId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate")
    List<UsageRecord> findBySubscriptionIdAndPeriod(
        @Param("subscriptionId") Long subscriptionId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    List<UsageRecord> findByUsageType(com.telecom.usage.domain.entity.UsageType usageType);
    
    List<UsageRecord> findByCdrSource(String cdrSource);
    
    @Query("SELECT COUNT(u) FROM UsageRecord u WHERE u.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);
}
