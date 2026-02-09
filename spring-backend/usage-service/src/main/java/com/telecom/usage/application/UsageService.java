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
    private final Random random = new Random();
    
    // ==================== CDR INGESTION ====================
    
    /**
     * Ingest CDR from external system with full validation and idempotency
     * 
     * Design Decision: Usage is linked to Subscription (not Customer) because:
     * 1. A customer can have multiple subscriptions
     * 2. Each subscription has its own plan/pricing
     * 3. Billing is calculated per subscription
     */
    @Transactional
    public Optional<CdrIngestionResponse> ingestCdr(CdrIngestionRequest request, String correlationId) {
        // Check for duplicate using sessionId
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
        
        // Create new usage record from CDR
        UsageRecord usage = UsageRecord.builder()
            .sessionId(request.sessionId())
            .subscriptionId(request.subscriptionId())
            .contratId(request.subscriptionId())  // Backward compatibility
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
        
        // Publish event for downstream processing (billing, analytics)
        eventPublisher.publishUsageRecorded(usage, correlationId, "1.0");
        idempotencyService.logIdempotencyCheck(request.sessionId(), false, request.cdrSource());
        
        return Optional.of(CdrIngestionResponse.success(usage, correlationId));
    }
    
    /**
     * Bulk CDR ingestion - processes each CDR independently
     */
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
    
    // ==================== LEGACY API SUPPORT ====================
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
