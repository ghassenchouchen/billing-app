package com.telecom.billing.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_INVOICE_CREATED = "billing.invoice.created";
    private static final String TOPIC_INVOICE_FINALIZED = "billing.invoice.finalized";
    private static final String TOPIC_INVOICE_SENT = "billing.invoice.sent";
    private static final String TOPIC_INVOICE_PAID = "billing.invoice.paid";
    private static final String TOPIC_INVOICE_OVERDUE = "billing.invoice.overdue";
    private static final String TOPIC_INVOICE_CANCELLED = "billing.invoice.cancelled";
    
    public void publishInvoiceCreated(Long invoiceId, String numeroFacture, Long clientId, BigDecimal montantTTC) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_CREATED",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "clientId", clientId,
                "montantTTC", montantTTC,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_CREATED, invoiceId.toString(), event);
            log.info("Published invoice created event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice created event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
    
    public void publishInvoiceFinalized(Long invoiceId, String numeroFacture, Long clientId, BigDecimal montantTTC) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_FINALIZED",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "clientId", clientId,
                "montantTTC", montantTTC,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_FINALIZED, invoiceId.toString(), event);
            log.info("Published invoice finalized event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice finalized event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
    
    public void publishInvoiceSent(Long invoiceId, String numeroFacture, Long clientId) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_SENT",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "clientId", clientId,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_SENT, invoiceId.toString(), event);
            log.info("Published invoice sent event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice sent event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
    
    public void publishInvoicePaid(Long invoiceId, String numeroFacture, Long clientId, BigDecimal montantPaid) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_PAID",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "clientId", clientId,
                "montantPaid", montantPaid,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_PAID, invoiceId.toString(), event);
            log.info("Published invoice paid event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice paid event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
    
    public void publishInvoiceOverdue(Long invoiceId, String numeroFacture, Long clientId, BigDecimal montantDue) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_OVERDUE",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "clientId", clientId,
                "montantDue", montantDue,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_OVERDUE, invoiceId.toString(), event);
            log.info("Published invoice overdue event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice overdue event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
    
    public void publishInvoiceCancelled(Long invoiceId, String numeroFacture, String reason) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "INVOICE_CANCELLED",
                "invoiceId", invoiceId,
                "numeroFacture", numeroFacture,
                "reason", reason,
                "timestamp", LocalDateTime.now().toString()
            );
            kafkaTemplate.send(TOPIC_INVOICE_CANCELLED, invoiceId.toString(), event);
            log.info("Published invoice cancelled event for invoice {}", numeroFacture);
        } catch (Exception e) {
            log.warn("Failed to publish invoice cancelled event for invoice {} - {}", numeroFacture, e.getMessage());
        }
    }
}
