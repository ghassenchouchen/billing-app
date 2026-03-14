package com.telecom.usage.web.controller;

import com.telecom.usage.application.UsageService;
import com.telecom.usage.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UsageController {
    
    private final UsageService usageService;
    
    @Tag(name = "CDR Ingestion")
    @Operation(summary = "Ingest CDR from external system")
    @PostMapping("/usage/cdr")
    public ResponseEntity<CdrIngestionResponse> ingestCdr(
            @Valid @RequestBody CdrIngestionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        log.info("CDR ingestion request - sessionId: {}, subscriptionId: {}, type: {}, source: {}, correlationId: {}",
            request.sessionId(), request.subscriptionId(), request.usageType(), request.cdrSource(), correlationId);
        
        return usageService.ingestCdr(request, correlationId)
            .map(response -> {
                if (response.duplicate()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .orElse(ResponseEntity.status(HttpStatus.CONFLICT).body(
                new CdrIngestionResponse(
                    null, request.sessionId(), request.subscriptionId(), request.serviceId(),
                    request.usageType(), request.quantity(), request.unit(), request.timestamp(),
                    null, request.cdrSource(), "DUPLICATE", true,
                    "CDR already exists with this session ID", correlationId
                )
            ));
    }
    
    @Tag(name = "CDR Ingestion")
    @Operation(summary = "Bulk CDR ingestion")
    @PostMapping("/usage/cdr/bulk")
    public ResponseEntity<List<CdrIngestionResponse>> ingestCdrBulk(
            @Valid @RequestBody List<CdrIngestionRequest> requests,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        log.info("Bulk CDR ingestion - count: {}, correlationId: {}", requests.size(), correlationId);
        
        List<CdrIngestionResponse> responses = usageService.ingestCdrBulk(requests, correlationId);
        return ResponseEntity.ok(responses);
    }
    
    
  
    
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get all usage records")
    @GetMapping("/usage")
    public ResponseEntity<List<UsageRecordDto>> getAllUsage() {
        return ResponseEntity.ok(usageService.getAllUsage());
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by subscription")
    @GetMapping("/usage/subscription/{subscriptionId}")
    public ResponseEntity<List<UsageRecordDto>> getUsageBySubscription(
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(usageService.getUsageByAbonnementId(subscriptionId));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by abonnement")
    @GetMapping("/usage/abonnement/{abonnementId}")
    public ResponseEntity<List<UsageRecordDto>> getUsageByAbonnementId(
            @PathVariable Long abonnementId) {
        return ResponseEntity.ok(usageService.getUsageByAbonnementId(abonnementId));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by period")
    @GetMapping("/usage/subscription/{subscriptionId}/period")
    public ResponseEntity<List<UsageRecordDto>> getUsageByPeriod(
            @PathVariable Long subscriptionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(usageService.getUsageByAbonnementIdAndPeriod(subscriptionId, startDate, endDate));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by period (abonnement)")
    @GetMapping("/usage/abonnement/{abonnementId}/period")
    public ResponseEntity<List<UsageRecordDto>> getUsageByPeriodAbonnement(
            @PathVariable Long abonnementId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(usageService.getUsageByAbonnementIdAndPeriod(abonnementId, startDate, endDate));
    }
    
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Record usage manually")
    @PostMapping("/usage")
    public ResponseEntity<UsageRecordDto> recordUsage(@Valid @RequestBody CreateUsageRequest request) {
        return ResponseEntity.ok(usageService.recordUsage(request));
    }
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Generate test usage")
    @PostMapping("/usage/generate")
    public ResponseEntity<List<UsageRecordDto>> generateUsage(@Valid @RequestBody GenerateUsageRequest request) {
        return ResponseEntity.ok(usageService.generateUsage(request));
    }
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Rate usage record")
    @PostMapping("/usage/{id}/rate")
    public ResponseEntity<UsageRecordDto> rateUsage(
            @PathVariable Long id,
            @RequestParam BigDecimal prixUnitaire) {
        return ResponseEntity.ok(usageService.rateUsage(id, prixUnitaire));
    }
}
