package com.telecom.usage.infrastructure.cdr;

import com.telecom.usage.application.IdempotencyService;
import com.telecom.usage.application.UsageService;
import com.telecom.usage.domain.entity.*;
import com.telecom.usage.domain.repository.RawUsageRecordRepository;
import com.telecom.usage.infrastructure.kafka.UsageEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class CdrFileIngestionService {
    
    @Value("${cdr.ingestion.incoming-directory:incoming}")
    private String incomingDirectory;
    
    @Value("${cdr.ingestion.processed-directory:processed}")
    private String processedDirectory;
    
    @Value("${cdr.ingestion.failed-directory:failed}")
    private String failedDirectory;
    
    @Value("${cdr.ingestion.batch-size:100}")
    private int batchSize;
    
    @Value("${cdr.ingestion.enabled:true}")
    private boolean ingestionEnabled;
    
    private final CdrCsvParser cdrCsvParser;
    private final CdrNormalizer cdrNormalizer;
    private final UsageService usageService;
    private final RawUsageRecordRepository rawUsageRecordRepository;
    private final UsageEventPublisher eventPublisher;
    
    /*
     * Scan incoming directory and process CDR files
     * Scheduled to run every 5 minutes
     */
    @Scheduled(fixedDelayString = "${cdr.ingestion.scan-interval:300000}", initialDelayString = "${cdr.ingestion.initial-delay:30000}")
    public void scanAndIngestCdrFiles() {
        if (!ingestionEnabled) {
            log.debug("CDR file ingestion is disabled");
            return;
        }
        
        try {
            ensureDirectoriesExist();
            
            File incomingDir = new File(incomingDirectory);
            if (!incomingDir.exists()) {
                log.debug("Incoming directory does not exist: {}", incomingDirectory);
                return;
            }
            
            File[] csvFiles = incomingDir.listFiles(csvFileFilter());
            if (csvFiles == null || csvFiles.length == 0) {
                log.trace("No CDR files found in incoming directory");
                return;
            }
            
            log.info("Found {} CDR files to process", csvFiles.length);
            
            for (File csvFile : csvFiles) {
                try {
                    processCdrFile(csvFile);
                } catch (Exception e) {
                    log.error("Error processing CDR file: {}", csvFile.getName(), e);
                    moveToDirWithRename(csvFile, failedDirectory, "ERROR_");
                }
            }
        } catch (Exception e) {
            log.error("Error during CDR file ingestion scan", e);
        }
    }
    
  
    @Transactional
    public void processCdrFile(File cdrFile) throws IOException {
        String cdrSource = cdrFile.getName().replaceAll("\\.csv$", "");
        String sessionId = UUID.randomUUID().toString();
        
        log.info("Processing CDR file: {} with sessionId: {}", cdrFile.getName(), sessionId);
        
        try {
            List<CdrRecord> cdrRecords = cdrCsvParser.parseCsvFile(cdrFile, cdrSource);
            log.info("Parsed {} records from CDR file: {}", cdrRecords.size(), cdrFile.getName());
            
            if (cdrRecords.isEmpty()) {
                log.warn("No valid CDR records found in file: {}", cdrFile.getName());
                moveToDirWithRename(cdrFile, processedDirectory, "EMPTY_");
                return;
            }
            
            int successCount = 0;
            int failureCount = 0;
            int duplicateCount = 0;
            
            for (CdrRecord cdrRecord : cdrRecords) {
                try {
                    RawUsageRecord rawRecord = RawUsageRecord.builder()
                        .externalId(cdrRecord.externalId())
                        .cdrSource(cdrSource)
                        .rawData(cdrRecord.rawLine())
                        .sessionId(sessionId)
                        .status(RawUsageRecord.CdrStatus.RECEIVED)
                        .build();
                    
                    Optional<RawUsageRecord> existing = rawUsageRecordRepository.findByExternalId(cdrRecord.externalId());
                    if (existing.isPresent()) {
                        rawRecord.setStatus(RawUsageRecord.CdrStatus.DUPLICATE);
                        rawUsageRecordRepository.save(rawRecord);
                        duplicateCount++;
                        log.debug("Duplicate CDR detected: {}", cdrRecord.externalId());
                        continue;
                    }
                    
                    CdrNormalizer.NormalizedUsage normalizedUsage = cdrNormalizer.normalize(cdrRecord, sessionId);
                    
                    if (!normalizedUsage.isSuccess()) {
                        rawRecord.setStatus(RawUsageRecord.CdrStatus.FAILED);
                        rawRecord.setErrorMessage(normalizedUsage.getErrorMessage());
                        rawUsageRecordRepository.save(rawRecord);
                        failureCount++;
                        log.warn("Failed to normalize CDR {}: {}", cdrRecord.externalId(), normalizedUsage.getErrorMessage());
                        continue;
                    }
                    
                    UsageRecord usageRecord = normalizedUsage.getUsageRecord();
                    usageRecord = usageService.saveUsageRecord(usageRecord);
                    
                    rawRecord.setStatus(RawUsageRecord.CdrStatus.MAPPED);
                    rawRecord.setUsageRecordId(usageRecord.getId());
                    rawUsageRecordRepository.save(rawRecord);
                    
                    // Publish event
                    eventPublisher.publishUsageRecorded(usageRecord, sessionId, "1.0");
                    
                    successCount++;
                    log.trace("Successfully processed CDR: {}", cdrRecord.externalId());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("Error processing CDR record {}: {}", cdrRecord.externalId(), e.getMessage());
                }
            }
            
            eventPublisher.publishCdrBatchProcessed(sessionId, successCount, cdrSource, sessionId);
            
            moveToDirWithRename(cdrFile, processedDirectory, "");
            
            log.info("CDR file processing complete - file: {}, success: {}, failure: {}, duplicate: {}", 
                cdrFile.getName(), successCount, failureCount, duplicateCount);
            
        } catch (Exception e) {
            log.error("Failed to process CDR file: {}", cdrFile.getName(), e);
            moveToDirWithRename(cdrFile, failedDirectory, "ERROR_");
            throw e;
        }
    }
    
   
    private void ensureDirectoriesExist() throws IOException {
        Files.createDirectories(Paths.get(incomingDirectory));
        Files.createDirectories(Paths.get(processedDirectory));
        Files.createDirectories(Paths.get(failedDirectory));
    }
    
 
    private FileFilter csvFileFilter() {
        return file -> file.isFile() && file.getName().toLowerCase().endsWith(".csv");
    }
    
    
    private void moveToDirWithRename(File file, String targetDir, String prefix) {
        try {
            File targetDirectory = new File(targetDir);
            if (!targetDirectory.exists()) {
                targetDirectory.mkdirs();
            }
            
            String newName = prefix + file.getName();
            File newFile = new File(targetDirectory, newName);
            
            if (file.renameTo(newFile)) {
                log.debug("Moved file from {} to {}", file.getAbsolutePath(), newFile.getAbsolutePath());
            } else {
                log.warn("Failed to move file to {}: {}", targetDir, file.getName());
            }
        } catch (Exception e) {
            log.error("Error moving file to {}: {}", targetDir, e.getMessage());
        }
    }
    
  
    public void manualTriggerIngestion() {
        log.info("Manual CDR ingestion trigger");
        scanAndIngestCdrFiles();
    }
    
    
    public void retryFailedCdrs() {
        log.info("Retrying failed CDR files");
        try {
            File failedDir = new File(failedDirectory);
            if (!failedDir.exists()) {
                return;
            }
            
            File[] failedFiles = failedDir.listFiles(file -> 
                file.isFile() && file.getName().startsWith("ERROR_") && file.getName().endsWith(".csv")
            );
            
            if (failedFiles == null || failedFiles.length == 0) {
                return;
            }
            
            log.info("Found {} failed CDR files to retry", failedFiles.length);
            
            for (File failedFile : failedFiles) {
                String originalName = failedFile.getName().replaceFirst("^ERROR_", "");
                File newFile = new File(incomingDirectory, originalName);
                
                if (failedFile.renameTo(newFile)) {
                    log.info("Moved failed file back to incoming: {}", originalName);
                }
            }
            
            scanAndIngestCdrFiles();
        } catch (Exception e) {
            log.error("Error retrying failed CDRs", e);
        }
    }
}
