package com.telecom.usage.infrastructure.cdr;

import com.telecom.usage.domain.entity.CdrRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service to parse CDR CSV files
 * Supports standard telecom CDR formats with customizable field mappings
 */
@Component
@Slf4j
public class CdrCsvParser {
    
    /**
     * Standard CDR CSV Header: external_id,contrat_id,service_id,quantity,date_usage,msisdn
     */
    private static final String[] EXPECTED_HEADERS = {
        "external_id", "contrat_id", "service_id", "quantity", "date_usage", "msisdn"
    };
    
    /**
     * Parse CDR CSV file and return list of CdrRecord
     */
    public List<CdrRecord> parseCsvFile(File cdrFile, String cdrSource) throws IOException {
        List<CdrRecord> records = new ArrayList<>();
        
        try (Reader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(cdrFile), StandardCharsets.UTF_8))) {
            
            CSVFormat csvFormat = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withTrim();
            
            try (CSVParser csvParser = new CSVParser(reader, csvFormat)) {
                int lineNum = 1; // Account for header
                
                for (CSVRecord record : csvParser) {
                    lineNum++;
                    
                    try {
                        CdrRecord cdrRecord = parseCsvRecord(record, cdrSource, cdrFile.getName() + ":" + lineNum);
                        if (cdrRecord != null) {
                            records.add(cdrRecord);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse CDR line {}: {}", lineNum, e.getMessage());
                        // Continue processing other records
                    }
                }
            }
        }
        
        log.info("Parsed {} CDR records from file: {}", records.size(), cdrFile.getName());
        return records;
    }
    
    /**
     * Parse individual CSV record into CdrRecord
     */
    private CdrRecord parseCsvRecord(CSVRecord record, String cdrSource, String lineReference) {
        try {
            String externalId = record.get("external_id");
            String contratIdStr = record.get("contrat_id");
            String serviceIdStr = record.get("service_id");
            String quantity = record.get("quantity");
            String dateUsage = record.get("date_usage");
            
            Long contratId = Long.parseLong(contratIdStr);
            Long serviceId = Long.parseLong(serviceIdStr);
            
            CdrRecord cdrRecord = new CdrRecord(
                externalId,
                contratId,
                serviceId,
                quantity,
                dateUsage,
                cdrSource,
                record.toString()
            );
            
            if (!cdrRecord.isValid()) {
                log.warn("Invalid CDR record at {}: {}", lineReference, cdrRecord.getValidationError());
                return null;
            }
            
            return cdrRecord;
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric field in CDR record at {}: {}", lineReference, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Error parsing CDR record at {}: {}", lineReference, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse CDR line string (for API requests)
     */
    public CdrRecord parseCdrLine(String csvLine, String cdrSource, String externalId) {
        String[] fields = csvLine.split(",");
        if (fields.length < 5) {
            log.warn("Invalid CDR line format - insufficient fields: {}", csvLine);
            return null;
        }
        
        try {
            CdrRecord cdrRecord = new CdrRecord(
                externalId,
                Long.parseLong(fields[0].trim()),  // contrat_id
                Long.parseLong(fields[1].trim()),  // service_id
                fields[2].trim(),                    // quantity
                fields[3].trim(),                    // date_usage
                cdrSource,
                csvLine
            );
            
            if (!cdrRecord.isValid()) {
                log.warn("Invalid CDR record: {}", cdrRecord.getValidationError());
                return null;
            }
            
            return cdrRecord;
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric field in CDR line: {}", e.getMessage());
            return null;
        }
    }
}
