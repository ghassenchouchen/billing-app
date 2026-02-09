package com.telecom.usage.infrastructure.cdr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/cdr")
@RequiredArgsConstructor
@Slf4j
public class CdrManagementController {
    
    private final CdrFileIngestionService cdrFileIngestionService;
    
  
    @PostMapping("/scan")
    public ResponseEntity<String> triggerCdrScan() {
        log.info("Manual CDR scan triggered");
        cdrFileIngestionService.manualTriggerIngestion();
        return ResponseEntity.ok("CDR ingestion scan triggered");
    }
    
    
    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailedCdrs() {
        log.info("Retry failed CDRs triggered");
        cdrFileIngestionService.retryFailedCdrs();
        return ResponseEntity.ok("Failed CDR retry triggered");
    }
    
   
    @GetMapping("/status")
    public ResponseEntity<String> getCdrStatus() {
        return ResponseEntity.ok("""
            CDR Ingestion Service Status:
            - Service is running and monitoring /incoming directory
            - CSV files are parsed, validated, and normalized
            - Usage records are persisted to database
            - Events are published to Kafka topic: usage.events
            - Processed files are moved to /processed or /failed directories
            """);
    }
}
