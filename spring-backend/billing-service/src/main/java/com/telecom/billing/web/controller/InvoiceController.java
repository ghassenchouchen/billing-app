package com.telecom.billing.web.controller;

import com.telecom.billing.application.BillingService;
import com.telecom.billing.application.InvoicePdfService;
import com.telecom.billing.domain.entity.Facture;
import com.telecom.billing.domain.entity.Facture.FactureStatus;
import com.telecom.billing.infrastructure.client.CustomerClient;
import com.telecom.billing.web.dto.FactureDto;
import com.telecom.billing.web.dto.GenerateInvoiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {
    
    private final BillingService billingService;
    private final InvoicePdfService pdfService;
    private final CustomerClient customerClient;
    
    @GetMapping
    public ResponseEntity<List<FactureDto>> getAllInvoices() {
        List<FactureDto> invoices = billingService.getAllInvoices().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FactureDto> getInvoiceById(@PathVariable Long id) {
        return billingService.getInvoiceById(id)
            .map(this::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/number/{numeroFacture}")
    public ResponseEntity<FactureDto> getInvoiceByNumber(@PathVariable String numeroFacture) {
        return billingService.getInvoiceByNumber(numeroFacture)
            .map(this::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FactureDto>> getInvoicesByClient(@PathVariable Long clientId) {
        List<FactureDto> invoices = billingService.getInvoicesByClient(clientId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/client/{clientId}/unpaid")
    public ResponseEntity<List<FactureDto>> getUnpaidInvoices(@PathVariable Long clientId) {
        List<FactureDto> invoices = billingService.getUnpaidInvoices(clientId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/client/{clientId}/balance")
    public ResponseEntity<BigDecimal> getOutstandingBalance(@PathVariable Long clientId) {
        return ResponseEntity.ok(billingService.getOutstandingBalance(clientId));
    }
    
    @GetMapping("/status/{statut}")
    public ResponseEntity<List<FactureDto>> getInvoicesByStatus(@PathVariable String statut) {
        List<FactureDto> invoices = billingService.getInvoicesByStatus(FactureStatus.valueOf(statut.toUpperCase())).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(invoices);
    }
    
    @PostMapping("/generate")
    public ResponseEntity<FactureDto> generateInvoice(@Valid @RequestBody GenerateInvoiceRequest request) {
        Facture facture = billingService.generateInvoiceForSubscription(
            request.abonnementId(),
            request.periodeDebut(),
            request.periodeFin()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(facture));
    }
    
    @PostMapping("/{id}/finalize")
    public ResponseEntity<FactureDto> finalizeInvoice(@PathVariable Long id) {
        Facture facture = billingService.finalizeInvoice(id);
        return ResponseEntity.ok(toDto(facture));
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<FactureDto> sendInvoice(@PathVariable Long id) {
        Facture facture = billingService.markInvoiceAsSent(id);
        return ResponseEntity.ok(toDto(facture));
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<FactureDto> markAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) String paymentReference) {
        Facture facture = billingService.markInvoiceAsPaid(id);
        return ResponseEntity.ok(toDto(facture));
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<FactureDto> cancelInvoice(
            @PathVariable Long id, 
            @RequestParam(defaultValue = "Annulation demandée") String reason) {
        Facture facture = billingService.cancelInvoice(id, reason);
        return ResponseEntity.ok(toDto(facture));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Facture facture = billingService.getInvoiceById(id)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
        
        byte[] pdfBytes = pdfService.generatePdf(facture);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            "facture-" + facture.getNumeroFacture() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    private FactureDto toDto(Facture facture) {
        var lines = facture.getLignes().stream()
            .map(line -> new FactureDto.InvoiceLineDto(
                line.getId(),
                line.getType() != null ? line.getType().name() : null,
                line.getDescription(),
                line.getServiceId(),
                line.getUsageId(),
                line.getQuantite(),
                line.getPrixUnitaire(),
                line.getMontant()
            ))
            .collect(Collectors.toList());
        
        // Resolve client name
        String clientName = null;
        try {
            CustomerClient.ClientDto client = customerClient.getCustomerById(facture.getClientId());
            clientName = client.fullName();
        } catch (Exception e) {
            log.warn("Could not resolve client name for id {}", facture.getClientId());
        }
        
        return new FactureDto(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getClientId(),
            clientName,
            facture.getAbonnementId(),
            facture.getDateFacture(),
            facture.getDateEcheance(),
            facture.getPeriodeDebut(),
            facture.getPeriodeFin(),
            facture.getMontantHT(),
            facture.getMontantTVA(),
            facture.getMontantTTC(),
            facture.getStatut().name(),
            facture.getLignes().size(),
            facture.getCreatedAt(),
            facture.getPaidAt(),
            lines
        );
    }
}
