package com.telecom.usage.application;

import com.telecom.usage.domain.entity.UsageRecord;
import com.telecom.usage.domain.entity.UsageType;
import com.telecom.usage.domain.repository.UsageRecordRepository;
import com.telecom.usage.infrastructure.kafka.UsageEventPublisher;
import com.telecom.usage.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final PrepaidQuotaEnforcer prepaidQuotaEnforcer;
    private final Random random = new Random();
    
    // ==================== CDR INGESTION ====================
    
    @Transactional
    public Optional<CdrIngestionResponse> ingestCdr(CdrIngestionRequest request, String correlationId) {
        if (idempotencyService.isUsageAlreadyRecorded(request.sessionId())) {
            Optional<UsageRecord> existing = idempotencyService.getExistingUsageRecord(request.sessionId());
            idempotencyService.logIdempotencyCheck(request.sessionId(), true, request.cdrSource());
            
            if (existing.isPresent()) {
                log.info("Duplicate CDR detected - sessionId: {}, existing usageId: {}", 
                    request.sessionId(), existing.get().getId());
                return Optional.of(CdrIngestionResponse.duplicate(existing.get(), correlationId));
            }
            return Optional.empty();
        }
        
        // Prepaid quota enforcement
        PrepaidQuotaEnforcer.EnforcementResult quotaResult = 
            prepaidQuotaEnforcer.enforce(request.subscriptionId(), request.usageType(), request.quantity());
        
        if (!quotaResult.permitted()) {
            log.warn("CDR rejected (quota) - sessionId: {}, subscriptionId: {}, reason: {}",
                request.sessionId(), request.subscriptionId(), quotaResult.rejectionReason());
            
            UsageRecord rejected = UsageRecord.builder()
                .sessionId(request.sessionId())
                .subscriptionId(request.subscriptionId())
                .abonnementId(request.subscriptionId())
                .serviceId(request.serviceId())
                .usageType(request.usageType())
                .quantite(request.quantity())
                .unit(request.unit())
                .dateUsage(request.timestamp())
                .cdrSource(request.cdrSource())
                .calledNumber(request.calledNumber())
                .callingNumber(request.callingNumber())
                .cellId(request.cellId())
                .cdrRawData(request.rawCdrData())
                .status(UsageRecord.UsageStatus.REJECTED)
                .build();
            rejected = usageRecordRepository.save(rejected);
            
            return Optional.of(new CdrIngestionResponse(
                rejected.getId(), request.sessionId(), request.subscriptionId(), request.serviceId(),
                request.usageType(), request.quantity(), request.unit(), request.timestamp(),
                rejected.getCreatedAt(), request.cdrSource(), "REJECTED", false,
                "Quota exhausted: " + quotaResult.rejectionReason(), correlationId
            ));
        }
        
        UsageRecord usage = UsageRecord.builder()
            .sessionId(request.sessionId())
            .subscriptionId(request.subscriptionId())
            .abonnementId(request.subscriptionId())
            .serviceId(request.serviceId())
            .usageType(request.usageType())
            .quantite(request.quantity())
            .unit(request.unit())
            .dateUsage(request.timestamp())
            .cdrSource(request.cdrSource())
            .calledNumber(request.calledNumber())
            .callingNumber(request.callingNumber())
            .cellId(request.cellId())
            .cdrRawData(request.rawCdrData())
            .status(UsageRecord.UsageStatus.RECORDED)
            .build();
        
        usage = usageRecordRepository.save(usage);
        
        log.info("CDR ingested - usageId: {}, sessionId: {}, subscriptionId: {}, type: {}, quantity: {} {}", 
            usage.getId(), request.sessionId(), request.subscriptionId(), 
            request.usageType(), request.quantity(), request.unit());
        
        eventPublisher.publishUsageRecorded(usage, correlationId, "1.0");
        idempotencyService.logIdempotencyCheck(request.sessionId(), false, request.cdrSource());
        
        return Optional.of(CdrIngestionResponse.success(usage, correlationId));
    }
    
    @Transactional
    public List<CdrIngestionResponse> ingestCdrBulk(List<CdrIngestionRequest> requests, String correlationId) {
        List<CdrIngestionResponse> responses = new ArrayList<>();
        
        for (CdrIngestionRequest request : requests) {
            try {
                Optional<CdrIngestionResponse> response = ingestCdr(request, correlationId);
                response.ifPresent(responses::add);
            } catch (Exception e) {
                log.error("Failed to ingest CDR - sessionId: {}, error: {}", request.sessionId(), e.getMessage());
                responses.add(new CdrIngestionResponse(
                    null, request.sessionId(), request.subscriptionId(), request.serviceId(),
                    request.usageType(), request.quantity(), request.unit(), request.timestamp(),
                    null, request.cdrSource(), "FAILED", false,
                    "Ingestion failed: " + e.getMessage(), correlationId
                ));
            }
        }
        
        log.info("Bulk CDR ingestion completed - total: {}, success: {}", 
            requests.size(), responses.stream().filter(r -> !r.duplicate() && !"FAILED".equals(r.status())).count());
        
        return responses;
    }
    
    // ==================== QUERY METHODS ====================
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getAllUsage() {
        return usageRecordRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageByAbonnementId(Long abonnementId) {
        return usageRecordRepository.findByAbonnementId(abonnementId).stream()
            .map(this::toDto)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UsageRecordDto> getUsageByAbonnementIdAndPeriod(
            Long abonnementId, LocalDateTime startDate, LocalDateTime endDate) {
        return usageRecordRepository.findByAbonnementIdAndPeriod(abonnementId, startDate, endDate)
            .stream()
            .map(this::toDto)
            .toList();
    }
    
    // ==================== LEGACY API SUPPORT ====================
    @Transactional
    public Optional<RecordUsageResponse> recordUsageWithIdempotency(
            RecordUsageRequest request, String correlationId, String eventVersion) {
        
        if (idempotencyService.isUsageAlreadyRecorded(request.sessionId())) {
            Optional<UsageRecord> existing = idempotencyService.getExistingUsageRecord(request.sessionId());
            idempotencyService.logIdempotencyCheck(request.sessionId(), true, request.cdrSource());
            
            if (existing.isPresent()) {
                return Optional.of(toResponse(existing.get(), true));
            }
            return Optional.empty();
        }
        
        UsageRecord usage = UsageRecord.builder()
            .sessionId(request.sessionId())
            .abonnementId(request.abonnementId())
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
    
    @Transactional
    public UsageRecordDto recordUsage(CreateUsageRequest request) {
        UsageRecord usage = UsageRecord.builder()
            .sessionId(UUID.randomUUID().toString())
            .abonnementId(request.abonnementId())
            .serviceId(request.serviceId())
            .quantite(request.quantite())
            .dateUsage(request.dateUsage() != null ? request.dateUsage() : LocalDateTime.now())
            .build();
        
        usage = usageRecordRepository.save(usage);
        log.info("Recorded usage: {} for abonnement: {}", usage.getId(), usage.getAbonnementId());
        
        eventPublisher.publishUsageRecorded(usage);
        
        return toDto(usage);
    }
    
    @Transactional
    public List<UsageRecordDto> generateUsage(GenerateUsageRequest request) {
        List<UsageRecord> generatedUsage = request.serviceIds().stream()
            .map(serviceId -> {
                UsageRecord usage = UsageRecord.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .abonnementId(request.abonnementId())
                    .serviceId(serviceId)
                    .quantite(BigDecimal.valueOf(random.nextInt(100) + 1))
                    .dateUsage(LocalDateTime.now())
                    .build();
                return usageRecordRepository.save(usage);
            })
            .toList();
        
        log.info("Generated {} usage records for abonnement: {}", 
            generatedUsage.size(), request.abonnementId());
        
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
            usage.getAbonnementId(),
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
            usage.getAbonnementId(),
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
