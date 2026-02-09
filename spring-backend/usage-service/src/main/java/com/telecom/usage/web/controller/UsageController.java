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
    
    // ==================== CDR INGESTION API ====================
    
    @Tag(name = "CDR Ingestion")
    @Operation(
        summary = "Ingest CDR from external system",
        description = """
            Primary endpoint for external mediation/network systems to submit CDR data.
            
            **Idempotency**: Uses sessionId to prevent duplicates. If a CDR with the same 
            sessionId already exists, returns HTTP 409 with the existing record.
            
            **Validation**: All required fields are validated. Invalid requests return HTTP 400.
            
            **Audit**: Raw CDR data is preserved for compliance and debugging.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "CDR successfully recorded",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CdrIngestionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate CDR - sessionId already exists",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CdrIngestionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation failed"
        )
    })
    @PostMapping("/usage/cdr")
    public ResponseEntity<CdrIngestionResponse> ingestCdr(
            @Valid @RequestBody 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "CDR data to ingest",
                required = true,
                content = @Content(
                    examples = @ExampleObject(
                        name = "Voice Call CDR",
                        value = """
                            {
                              "sessionId": "CDR-2026-02-05-123456-001",
                              "subscriptionId": 1001,
                              "serviceId": 1,
                              "usageType": "VOICE",
                              "quantity": 180.0,
                              "unit": "seconds",
                              "timestamp": "2026-02-05T14:30:00",
                              "cdrSource": "MSC-TUNIS-01",
                              "callingNumber": "+21697654321",
                              "calledNumber": "+21698123456",
                              "cellId": "CELL-TUN-001"
                            }
                            """
                    )
                )
            )
            CdrIngestionRequest request,
            
            @Parameter(description = "Correlation ID for distributed tracing")
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            
            @Parameter(description = "Idempotency key (alternative to sessionId in body)")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        
        // Generate correlation ID if not provided
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
    @Operation(
        summary = "Bulk CDR ingestion",
        description = "Ingest multiple CDRs in a single request. Each CDR is processed independently."
    )
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
    
    // ==================== LEGACY API (Backward Compatibility) ====================
    
    @Tag(name = "CDR Ingestion")
    @Operation(
        summary = "Record usage (Legacy API)",
        description = "Legacy endpoint maintained for backward compatibility. Use /usage/cdr for new integrations."
    )
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
    
    // ==================== USAGE QUERY API ====================
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get all usage records", description = "Retrieve all usage records (paginated in production)")
    @GetMapping("/usage")
    public ResponseEntity<List<UsageRecordDto>> getAllUsage() {
        return ResponseEntity.ok(usageService.getAllUsage());
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by subscription", description = "Retrieve all usage for a specific subscription")
    @GetMapping("/usage/subscription/{subscriptionId}")
    public ResponseEntity<List<UsageRecordDto>> getUsageBySubscription(
            @Parameter(description = "Subscription ID") @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(usageService.getUsageByContratId(subscriptionId));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by contract (legacy)", description = "Legacy endpoint - use /usage/subscription/{id}")
    @GetMapping("/usage/contrat/{contratId}")
    public ResponseEntity<List<UsageRecordDto>> getUsageByContratId(
            @Parameter(description = "Contract ID") @PathVariable Long contratId) {
        return ResponseEntity.ok(usageService.getUsageByContratId(contratId));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by period", description = "Retrieve usage for a subscription within a date range")
    @GetMapping("/usage/subscription/{subscriptionId}/period")
    public ResponseEntity<List<UsageRecordDto>> getUsageByPeriod(
            @Parameter(description = "Subscription ID") @PathVariable Long subscriptionId,
            @Parameter(description = "Start date (ISO format)", example = "2026-02-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", example = "2026-02-28T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(usageService.getUsageByContratIdAndPeriod(subscriptionId, startDate, endDate));
    }
    
    @Tag(name = "Usage Query")
    @Operation(summary = "Get usage by period (legacy)")
    @GetMapping("/usage/contrat/{contratId}/period")
    public ResponseEntity<List<UsageRecordDto>> getUsageByPeriodLegacy(
            @PathVariable Long contratId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(usageService.getUsageByContratIdAndPeriod(contratId, startDate, endDate));
    }
    
    // ==================== USAGE MANAGEMENT API ====================
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Record usage manually", description = "Administrative endpoint to manually record usage")
    @PostMapping("/usage")
    public ResponseEntity<UsageRecordDto> recordUsage(@RequestBody CreateUsageRequest request) {
        return ResponseEntity.ok(usageService.recordUsage(request));
    }
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Generate test usage", description = "Generate random usage for testing purposes")
    @PostMapping("/usage/generate")
    public ResponseEntity<List<UsageRecordDto>> generateUsage(@RequestBody GenerateUsageRequest request) {
        return ResponseEntity.ok(usageService.generateUsage(request));
    }
    
    @Tag(name = "Usage Management")
    @Operation(summary = "Rate usage record", description = "Apply rating (price) to a usage record")
    @PostMapping("/usage/{id}/rate")
    public ResponseEntity<UsageRecordDto> rateUsage(
            @Parameter(description = "Usage record ID") @PathVariable Long id,
            @Parameter(description = "Unit price to apply") @RequestParam BigDecimal prixUnitaire) {
        return ResponseEntity.ok(usageService.rateUsage(id, prixUnitaire));
    }
}

