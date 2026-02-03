package com.telecom.usage.web.controller;

import com.telecom.usage.application.UsageService;
import com.telecom.usage.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UsageController {
    
    private final UsageService usageService;
    
    /**
     * POST /api/v1/usage - Record usage from external mediation/network system
     * Represents an external system sending CDR data via REST API
     * Supports idempotency using sessionId
     */
    @PostMapping("/api/v1/usage")
    public ResponseEntity<RecordUsageResponse> recordUsageFromExternalSystem(
            @Valid @RequestBody RecordUsageRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader(value = "X-Event-Version", required = false, defaultValue = "1.0") String eventVersion) {
        
        return usageService.recordUsageWithIdempotency(request, correlationId, eventVersion)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .orElse(ResponseEntity.status(HttpStatus.CONFLICT).body(
                new RecordUsageResponse(
                    null, request.sessionId(), request.contratId(), request.serviceId(),
                    request.quantity(), null, null, request.dateUsage(), request.cdrSource(),
                    "DUPLICATE", false, "Usage already recorded with this session ID", true
                )
            ));
    }
    
    /**
     * Legacy endpoints for backward compatibility
     */
    @GetMapping("/usage")
    public ResponseEntity<List<UsageRecordDto>> getAllUsage() {
        return ResponseEntity.ok(usageService.getAllUsage());
    }
    
    @GetMapping("/usage/contrat/{contratId}")
    public ResponseEntity<List<UsageRecordDto>> getUsageByContratId(@PathVariable Long contratId) {
        return ResponseEntity.ok(usageService.getUsageByContratId(contratId));
    }
    
    @GetMapping("/usage/contrat/{contratId}/period")
    public ResponseEntity<List<UsageRecordDto>> getUsageByPeriod(
            @PathVariable Long contratId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(usageService.getUsageByContratIdAndPeriod(contratId, startDate, endDate));
    }
    
    @PostMapping("/usage")
    public ResponseEntity<UsageRecordDto> recordUsage(@RequestBody CreateUsageRequest request) {
        return ResponseEntity.ok(usageService.recordUsage(request));
    }
    
    @PostMapping("/usage/generate")
    public ResponseEntity<List<UsageRecordDto>> generateUsage(@RequestBody GenerateUsageRequest request) {
        return ResponseEntity.ok(usageService.generateUsage(request));
    }
    
    @PostMapping("/usage/{id}/rate")
    public ResponseEntity<UsageRecordDto> rateUsage(
            @PathVariable Long id,
            @RequestParam BigDecimal prixUnitaire) {
        return ResponseEntity.ok(usageService.rateUsage(id, prixUnitaire));
    }
}

