package com.telecom.authentication.infrastructure.config;

import com.telecom.authentication.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task to clean up expired and revoked refresh tokens.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Run daily at 3:00 AM to clean up old tokens.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Expired and revoked refresh tokens cleaned up");
    }
}
