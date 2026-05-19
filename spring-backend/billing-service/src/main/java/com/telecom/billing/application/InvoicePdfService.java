package com.telecom.billing.application;

import com.telecom.billing.domain.entity.Facture;
import com.telecom.billing.domain.entity.InvoiceLine;
import com.telecom.billing.infrastructure.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final TemplateEngine templateEngine;
    private final CustomerClient customerClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generatePdf(Facture facture) {
        Context ctx = new Context();

        // Invoice header
        ctx.setVariable("invoiceNumber", facture.getNumeroFacture());
        ctx.setVariable("invoiceDate", facture.getDateFacture().format(DATE_FMT));
        ctx.setVariable("dueDate", facture.getDateEcheance().format(DATE_FMT));
        ctx.setVariable("status", facture.getStatut().name());

        if (facture.getPeriodeDebut() != null && facture.getPeriodeFin() != null) {
            ctx.setVariable("periodStart", facture.getPeriodeDebut().format(DATE_FMT));
            ctx.setVariable("periodEnd", facture.getPeriodeFin().format(DATE_FMT));
        }

        // Amounts
        ctx.setVariable("montantHT", formatAmount(facture.getMontantHT()));
        ctx.setVariable("montantTVA", formatAmount(facture.getMontantTVA()));
        ctx.setVariable("montantTTC", formatAmount(facture.getMontantTTC()));

        // Customer info
        Map<String, String> customer = resolveCustomer(facture.getClientId());
        ctx.setVariable("clientName", customer.get("name"));
        ctx.setVariable("clientRef", customer.get("ref"));
        ctx.setVariable("clientAddress", customer.get("address"));
        ctx.setVariable("clientEmail", customer.get("email"));
        ctx.setVariable("clientPhone", customer.get("phone"));

        // Invoice lines
        List<Map<String, String>> lines = facture.getLignes().stream().map(line -> {
            Map<String, String> m = new HashMap<>();
            m.put("description", line.getDescription() != null ? line.getDescription() : "—");
            m.put("type", line.getType() != null ? line.getType().name() : "");
            m.put("quantity", String.valueOf(line.getQuantite()));
            m.put("unitPrice", formatAmount(line.getPrixUnitaire()));
            m.put("total", formatAmount(line.getMontant()));
            return m;
        }).toList();
        ctx.setVariable("lines", lines);

        // Render HTML from Thymeleaf template
        String html = templateEngine.process("invoice", ctx);

        // Convert to PDF
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os, true);
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", facture.getNumeroFacture(), e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private Map<String, String> resolveCustomer(Long clientId) {
        Map<String, String> result = new HashMap<>();
        try {
            CustomerClient.ClientDto dto = customerClient.getCustomerById(clientId);
            result.put("name", dto.fullName());
            result.put("ref", dto.customerRef());
            result.put("address", dto.fullAddress());
            result.put("email", dto.email() != null ? dto.email() : "—");
            result.put("phone", dto.telephone() != null ? dto.telephone() : "—");
        } catch (Exception e) {
            log.warn("Could not resolve customer {}: {}", clientId, e.getMessage());
            result.put("name", "Client #" + clientId);
            result.put("ref", "—");
            result.put("address", "—");
            result.put("email", "—");
            result.put("phone", "—");
        }
        return result;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.000";
        return amount.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
