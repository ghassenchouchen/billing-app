package com.telecom.usage.infrastructure.cdr;

import com.telecom.usage.domain.entity.CdrRecord;
import com.telecom.usage.domain.entity.UsageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service to normalize and validate parsed CDR records
 * Converts CdrRecord to UsageRecord domain entity
 */
@Component
@Slf4j
public class CdrNormalizer {
    
    private static final DateTimeFormatter[] SUPPORTED_FORMATS = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,           
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),  
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),       
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), 
        DateTimeFormatter.ofPattern("dd/MM/yyyy")        
    };
    
    /*
     * Normalize CDR record to UsageRecord entity
     */
    public NormalizedUsage normalize(CdrRecord cdr, String sessionId) {
        List<String> errors = new ArrayList<>();
        
        // Validate contrat ID
        if (cdr.contratId() == null || cdr.contratId() <= 0) {
            errors.add("Invalid contract ID: " + cdr.contratId());
        }
        
        // Validate service ID
        if (cdr.serviceId() == null || cdr.serviceId() <= 0) {
            errors.add("Invalid service ID: " + cdr.serviceId());
        }
        
        // Parse and validate quantity
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(cdr.quantity());
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Quantity must be greater than 0: " + quantity);
            }
        } catch (NumberFormatException e) {
            errors.add("Invalid quantity format: " + cdr.quantity());
            quantity = null;
        }
        
        // Parse and validate date usage
        LocalDateTime dateUsage = parseDateTime(cdr.dateUsage());
        if (dateUsage == null) {
            errors.add("Invalid date format: " + cdr.dateUsage() + " - supported formats: ISO, yyyy-MM-dd HH:mm:ss");
        }
        
        if (!errors.isEmpty()) {
            return NormalizedUsage.failed(
                cdr.externalId(),
                String.join("; ", errors),
                cdr.rawLine()
            );
        }
        
        // Build normalized usage record
        UsageRecord usageRecord = UsageRecord.builder()
            .sessionId(sessionId)
            .contratId(cdr.contratId())
            .serviceId(cdr.serviceId())
            .quantite(quantity)
            .dateUsage(dateUsage)
            .cdrSource(cdr.cdrSource())
            .cdrRawData(cdr.rawLine())
            .status(UsageRecord.UsageStatus.NORMALIZED)
            .build();
        
        return NormalizedUsage.success(cdr.externalId(), usageRecord);
    }
    
   
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : SUPPORTED_FORMATS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
            }
        }
        
        log.warn("Unable to parse date: {} with any supported format", dateStr);
        return null;
    }
    
 
    public static class NormalizedUsage {
        private final String externalId;
        private final boolean success;
        private final UsageRecord usageRecord;
        private final String errorMessage;
        private final String rawData;
        
        private NormalizedUsage(String externalId, boolean success, UsageRecord usageRecord, 
                               String errorMessage, String rawData) {
            this.externalId = externalId;
            this.success = success;
            this.usageRecord = usageRecord;
            this.errorMessage = errorMessage;
            this.rawData = rawData;
        }
        
        public static NormalizedUsage success(String externalId, UsageRecord usageRecord) {
            return new NormalizedUsage(externalId, true, usageRecord, null, null);
        }
        
        public static NormalizedUsage failed(String externalId, String errorMessage, String rawData) {
            return new NormalizedUsage(externalId, false, null, errorMessage, rawData);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public UsageRecord getUsageRecord() {
            return usageRecord;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getExternalId() {
            return externalId;
        }
        
        public String getRawData() {
            return rawData;
        }
    }
}
