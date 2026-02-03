package com.telecom.usage.application;

import com.telecom.usage.domain.entity.RawUsageRecord;
import com.telecom.usage.domain.entity.UsageRecord;
import com.telecom.usage.domain.repository.RawUsageRecordRepository;
import com.telecom.usage.domain.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to handle idempotency using sessionId to prevent duplicate usage records
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    
    private final UsageRecordRepository usageRecordRepository;
    private final RawUsageRecordRepository rawUsageRecordRepository;
    
    /**
     * Check if a usage has already been recorded with this sessionId
     */
    @Transactional(readOnly = true)
    public boolean isUsageAlreadyRecorded(String sessionId) {
        return usageRecordRepository.existsBySessionId(sessionId);
    }
    
    /**
     * Get existing usage record if already recorded
     */
    @Transactional(readOnly = true)
    public Optional<UsageRecord> getExistingUsageRecord(String sessionId) {
        return usageRecordRepository.findBySessionId(sessionId);
    }
    
    /**
     * Check if raw CDR has already been processed
     */
    @Transactional(readOnly = true)
    public boolean isCdrAlreadyProcessed(String externalId) {
        return rawUsageRecordRepository.findByExternalId(externalId).isPresent();
    }
    
    /**
     * Get existing raw CDR if already processed
     */
    @Transactional(readOnly = true)
    public Optional<RawUsageRecord> getExistingCdr(String externalId) {
        return rawUsageRecordRepository.findByExternalId(externalId);
    }
    
    /**
     * Check if session has already been fully processed
     */
    @Transactional(readOnly = true)
    public boolean isSessionFullyProcessed(String sessionId) {
        long pendingCount = rawUsageRecordRepository.countPendingBySessionId(sessionId);
        return pendingCount == 0;
    }
    
    /**
     * Get count of records processed for a session
     */
    @Transactional(readOnly = true)
    public long getProcessedRecordCountForSession(String sessionId) {
        return usageRecordRepository.countBySessionId(sessionId);
    }
    
    /**
     * Log idempotency check result
     */
    public void logIdempotencyCheck(String sessionId, boolean isDuplicate, String source) {
        if (isDuplicate) {
            log.warn("Duplicate usage detected - sessionId: {}, source: {}", sessionId, source);
        } else {
            log.debug("New usage accepted - sessionId: {}, source: {}", sessionId, source);
        }
    }
}
