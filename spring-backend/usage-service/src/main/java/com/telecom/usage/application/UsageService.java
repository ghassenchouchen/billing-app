package com.telecom.usage.application;

import com.telecom.usage.domain.entity.UsageRecord;
import com.telecom.usage.domain.repository.UsageRecordRepository;
import com.telecom.usage.infrastructure.kafka.UsageEventPublisher;
import com.telecom.usage.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageService {
    
    private final UsageRecordRepository usageRecordRepository;
    private final UsageEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final Random random = new Random();
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getAllUsage() {
        return usageRecordRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageByContratId(Long contratId) {
        return usageRecordRepository.findByContratId(contratId).stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageByContratIdAndPeriod(
            Long contratId, LocalDateTime startDate, LocalDateTime endDate) {
        return usageRecordRepository.findByContratIdAndPeriod(contratId, startDate, endDate)
            .stream()
            .map(this::toDto)
            .toList();
    }
    
    /**
     * Record usage from external system with idempotency check
     * Returns Optional.empty() if duplicate, RecordUsageResponse otherwise
     */
    @Transactional
    public Optional<RecordUsageResponse> recordUsageWithIdempotency(
            RecordUsageRequest request, String correlationId, String eventVersion) {
        
        // Check for duplicate using sessionId
        if (idempotencyService.isUsageAlreadyRecorded(request.sessionId())) {
            Optional<UsageRecord> existing = idempotencyService.getExistingUsageRecord(request.sessionId());
            idempotencyService.logIdempotencyCheck(request.sessionId(), true, request.cdrSource());
            
            if (existing.isPresent()) {
                return Optional.of(toResponse(existing.get(), true));
            }
            
            return Optional.empty();
        }
        
        // Create new usage record
        UsageRecord usage = UsageRecord.builder()
            .sessionId(request.sessionId())
            .contratId(request.contratId())
            .serviceId(request.serviceId())
            .quantite(request.quantity())
            .dateUsage(request.dateUsage() != null ? request.dateUsage() : LocalDateTime.now())
            .cdrSource(request.cdrSource())
            .cdrRawData(request.rawCdrData())
            .status(UsageRecord.UsageStatus.RECORDED)
            .build();
        
        usage = usageRecordRepository.save(usage);
        
        log.info("Recorded usage with idempotency - id: {}, sessionId: {}, cdrSource: {}", 
            usage.getId(), request.sessionId(), request.cdrSource());
        
        // Publish event with correlation ID
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        if (eventVersion == null) {
            eventVersion = "1.0";
        }
        
        eventPublisher.publishUsageRecorded(usage, correlationId, eventVersion);
        idempotencyService.logIdempotencyCheck(request.sessionId(), false, request.cdrSource());
        
        return Optional.of(toResponse(usage, false));
    }
    
    /**
     * Save usage record (for CDR file processing)
     */
    @Transactional
    public UsageRecord saveUsageRecord(UsageRecord usage) {
        // Ensure sessionId is set
        if (usage.getSessionId() == null || usage.getSessionId().isEmpty()) {
            usage.setSessionId(UUID.randomUUID().toString());
        }
        
        return usageRecordRepository.save(usage);
    }
    
    @Transactional
    public UsageRecordDto recordUsage(CreateUsageRequest request) {
        UsageRecord usage = UsageRecord.builder()
            .sessionId(UUID.randomUUID().toString())
            .contratId(request.contratId())
            .serviceId(request.serviceId())
            .quantite(request.quantite())
            .dateUsage(request.dateUsage() != null ? request.dateUsage() : LocalDateTime.now())
            .build();
        
        usage = usageRecordRepository.save(usage);
        log.info("Recorded usage: {} for contrat: {}", usage.getId(), usage.getContratId());
        
        // Publish event for billing service
        eventPublisher.publishUsageRecorded(usage);
        
        return toDto(usage);
    }
    
    @Transactional
    public List<UsageRecordDto> generateUsage(GenerateUsageRequest request) {
        List<UsageRecord> generatedUsage = request.serviceIds().stream()
            .map(serviceId -> {
                UsageRecord usage = UsageRecord.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .contratId(request.contratId())
                    .serviceId(serviceId)
                    .quantite(BigDecimal.valueOf(random.nextInt(100) + 1))
                    .dateUsage(LocalDateTime.now())
                    .build();
                return usageRecordRepository.save(usage);
            })
            .toList();
        
        log.info("Generated {} usage records for contrat: {}", 
            generatedUsage.size(), request.contratId());
        
        // Publish events
        generatedUsage.forEach(eventPublisher::publishUsageRecorded);
        
        return generatedUsage.stream().map(this::toDto).toList();
    }
    
    @Transactional
    public UsageRecordDto rateUsage(Long usageId, BigDecimal prixUnitaire) {
        UsageRecord usage = usageRecordRepository.findById(usageId)
            .orElseThrow(() -> new RuntimeException("Usage not found: " + usageId));
        
        usage.rate(prixUnitaire);
        usage.setStatus(UsageRecord.UsageStatus.RATED);
        usage = usageRecordRepository.save(usage);
        
        log.info("Rated usage: {} with price: {}", usageId, prixUnitaire);
        
        return toDto(usage);
    }
    
    private UsageRecordDto toDto(UsageRecord usage) {
        return new UsageRecordDto(
            usage.getId(),
            usage.getContratId(),
            usage.getServiceId(),
            usage.getQuantite(),
            usage.getPrixUnitaire(),
            usage.getMontantTotal(),
            usage.getDateUsage(),
            usage.isRated()
        );
    }
    
    private RecordUsageResponse toResponse(UsageRecord usage, boolean isDuplicate) {
        return new RecordUsageResponse(
            usage.getId(),
            usage.getSessionId(),
            usage.getContratId(),
            usage.getServiceId(),
            usage.getQuantite(),
            usage.getPrixUnitaire(),
            usage.getMontantTotal(),
            usage.getDateUsage(),
            usage.getCdrSource(),
            usage.getStatus() != null ? usage.getStatus().name() : "RECORDED",
            usage.isRated(),
            isDuplicate ? "Usage already recorded" : "Usage recorded successfully",
            isDuplicate
        );
    }
}
