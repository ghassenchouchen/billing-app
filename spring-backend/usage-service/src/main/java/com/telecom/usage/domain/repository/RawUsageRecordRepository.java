package com.telecom.usage.domain.repository;

import com.telecom.usage.domain.entity.RawUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RawUsageRecordRepository extends JpaRepository<RawUsageRecord, Long> {
    
    /**
     * Find by external ID for idempotency check
     */
    Optional<RawUsageRecord> findByExternalId(String externalId);
    
    /**
     * Find by session ID to track batch processing
     */
    List<RawUsageRecord> findBySessionId(String sessionId);
    
    /**
     * Find by status for processing pipelines
     */
    List<RawUsageRecord> findByStatus(RawUsageRecord.CdrStatus status);
    
    /**
     * Find by CDR source for auditing
     */
    List<RawUsageRecord> findByCdrSource(String cdrSource);
    
    /**
     * Find failed records for retry logic
     */
    @Query("SELECT r FROM RawUsageRecord r WHERE r.status = 'FAILED' AND r.createdAt >= :since")
    List<RawUsageRecord> findFailedRecordsSince(@Param("since") LocalDateTime since);
    
    /**
     * Count records by session and status
     */
    @Query("SELECT COUNT(r) FROM RawUsageRecord r WHERE r.sessionId = :sessionId AND r.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") String sessionId, @Param("status") RawUsageRecord.CdrStatus status);
    
    /**
     * Check if batch is fully processed
     */
    @Query("SELECT COUNT(r) FROM RawUsageRecord r WHERE r.sessionId = :sessionId AND r.status NOT IN ('MAPPED', 'FAILED', 'DUPLICATE')")
    long countPendingBySessionId(@Param("sessionId") String sessionId);
}
