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
    
    List<UsageRecord> findByContratId(Long contratId);
    
    List<UsageRecord> findByContratIdAndRatedFalse(Long contratId);
    
    /**
     * Find by sessionId for idempotency
     */
    Optional<UsageRecord> findBySessionId(String sessionId);
    
    /**
     * Check if usage already exists for this session
     */
    boolean existsBySessionId(String sessionId);
    
    @Query("SELECT u FROM UsageRecord u WHERE u.contratId = :contratId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate")
    List<UsageRecord> findByContratIdAndPeriod(
        @Param("contratId") Long contratId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT u FROM UsageRecord u WHERE u.contratId = :contratId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate AND u.rated = true")
    List<UsageRecord> findRatedByContratIdAndPeriod(
        @Param("contratId") Long contratId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find by subscription ID
     */
    List<UsageRecord> findBySubscriptionId(Long subscriptionId);
    
    /**
     * Find by subscription ID and period
     */
    @Query("SELECT u FROM UsageRecord u WHERE u.subscriptionId = :subscriptionId " +
           "AND u.dateUsage >= :startDate AND u.dateUsage < :endDate")
    List<UsageRecord> findBySubscriptionIdAndPeriod(
        @Param("subscriptionId") Long subscriptionId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find by usage type
     */
    List<UsageRecord> findByUsageType(com.telecom.usage.domain.entity.UsageType usageType);
    
    /**
     * Find by CDR source for auditing
     */
    List<UsageRecord> findByCdrSource(String cdrSource);
    
    /**
     * Count recorded usage for a session
     */
    @Query("SELECT COUNT(u) FROM UsageRecord u WHERE u.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);
}
