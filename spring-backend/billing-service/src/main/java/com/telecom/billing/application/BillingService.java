package com.telecom.billing.application;

import com.telecom.billing.domain.entity.Facture;
import com.telecom.billing.domain.entity.Facture.FactureStatus;
import com.telecom.billing.domain.entity.InvoiceLine;
import com.telecom.billing.domain.repository.FactureRepository;
import com.telecom.billing.infrastructure.client.CatalogClient;
import com.telecom.billing.infrastructure.client.CatalogClient.OffreDto;
import com.telecom.billing.infrastructure.client.SubscriptionClient;
import com.telecom.billing.infrastructure.client.SubscriptionClient.AbonnementDto;
import com.telecom.billing.infrastructure.client.UsageClient;
import com.telecom.billing.infrastructure.client.UsageClient.UsageRecordDto;
import com.telecom.billing.infrastructure.kafka.InvoiceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {
    
    private final FactureRepository factureRepository;
    private final SubscriptionClient subscriptionClient;
    private final CatalogClient catalogClient;
    private final UsageClient usageClient;
    private final InvoiceEventPublisher eventPublisher;
    
    @Value("${billing.tax-rate:0.0}")
    private BigDecimal taxRate;
    
    @Value("${billing.due-days:15}")
    private int dueDays;
    
    public List<Facture> getAllInvoices() {
        return factureRepository.findAll();
    }
    
    public Optional<Facture> getInvoiceById(Long id) {
        return factureRepository.findById(id);
    }
    
    public Optional<Facture> getInvoiceByNumber(String numeroFacture) {
        return factureRepository.findByNumeroFacture(numeroFacture);
    }
    
    public List<Facture> getInvoicesByClient(Long clientId) {
        return factureRepository.findByClientId(clientId);
    }
    
    public List<Facture> getInvoicesByStatus(FactureStatus statut) {
        return factureRepository.findByStatut(statut);
    }
    
    public List<Facture> getUnpaidInvoices(Long clientId) {
        return factureRepository.findByClientIdAndStatut(clientId, FactureStatus.SENT);
    }
    
    public BigDecimal getOutstandingBalance(Long clientId) {
        return factureRepository.calculateOutstandingBalance(clientId);
    }
    
    /**
     * Generate invoice for a specific subscription.
     * Period calculated based on subscription's billing frequency.
     */
    public Facture generateInvoiceForSubscription(Long abonnementId, LocalDate periodeDebut, LocalDate periodeFin) {
        log.info("Generating invoice for subscription {} - period {} to {}", abonnementId, periodeDebut, periodeFin);
        
        AbonnementDto abonnement = subscriptionClient.getAbonnement(abonnementId);
        
        Optional<Facture> existing = factureRepository.findByClientIdAndPeriod(
            abonnement.clientId(), periodeDebut, periodeFin
        );
        if (existing.isPresent()) {
            log.warn("Invoice already exists for client {} period {} - {}", 
                abonnement.clientId(), periodeDebut, periodeFin);
            return existing.get();
        }
        
        Facture facture = Facture.builder()
            .numeroFacture(generateInvoiceNumber())
            .clientId(abonnement.clientId())
            .abonnementId(abonnementId)
            .dateFacture(LocalDate.now())
            .dateEcheance(LocalDate.now().plusDays(dueDays))
            .periodeDebut(periodeDebut)
            .periodeFin(periodeFin)
            .statut(FactureStatus.DRAFT)
            .build();
        
        // Add subscription line
        try {
            OffreDto offre = catalogClient.getOffreById(abonnement.offreId());
            BigDecimal prixMensuel = offre.prixMensuel();
            if (prixMensuel != null && prixMensuel.compareTo(BigDecimal.ZERO) > 0) {
                long monthsInPeriod = java.time.temporal.ChronoUnit.MONTHS.between(periodeDebut, periodeFin.plusDays(1));
                if (monthsInPeriod <= 0) {
                    monthsInPeriod = 1;
                }
                BigDecimal totalSubscriptionAmount = prixMensuel.multiply(BigDecimal.valueOf(monthsInPeriod));
                
                InvoiceLine subscriptionLine = InvoiceLine.builder()
                    .type(InvoiceLine.LineType.SUBSCRIPTION)
                    .description(String.format("Abonnement - %s (%d mois: %s à %s)", 
                        offre.libelle(), monthsInPeriod, periodeDebut, periodeFin))
                    .quantite((int) monthsInPeriod)
                    .prixUnitaire(prixMensuel)
                    .montant(totalSubscriptionAmount)
                    .build();
                facture.addLigne(subscriptionLine);
            }
        } catch (Exception e) {
            log.warn("Could not fetch offer details for offreId {}: {}", abonnement.offreId(), e.getMessage());
        }
        
        // Add usage lines
        try {
            List<UsageRecordDto> usageRecords = usageClient.getUsageByPeriod(
                abonnementId, 
                periodeDebut.toString(), 
                periodeFin.toString()
            );
            
            for (UsageRecordDto usage : usageRecords) {
                InvoiceLine.LineType lineType = mapUsageType(usage.type());
                InvoiceLine usageLine = InvoiceLine.builder()
                    .type(lineType)
                    .description(formatUsageDescription(usage))
                    .usageId(usage.id())
                    .quantite(usage.quantite())
                    .prixUnitaire(usage.montant().divide(BigDecimal.valueOf(usage.quantite()), 4, java.math.RoundingMode.HALF_UP))
                    .montant(usage.montant())
                    .build();
                facture.addLigne(usageLine);
            }
        } catch (Exception e) {
            log.warn("Could not fetch usage data for subscription {}: {}", abonnementId, e.getMessage());
        }
        
        facture.calculateTotals(taxRate);
        facture = factureRepository.save(facture);
        
        eventPublisher.publishInvoiceCreated(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getClientId(),
            facture.getMontantTTC()
        );
        
        log.info("Generated invoice {} for client {} with total {}", 
            facture.getNumeroFacture(), facture.getClientId(), facture.getMontantTTC());
        
        return facture;
    }
    
    public Facture finalizeInvoice(Long invoiceId) {
        Facture facture = factureRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        if (facture.getStatut() != FactureStatus.DRAFT) {
            throw new RuntimeException("Only draft invoices can be finalized");
        }
        
        facture.finalize();
        facture = factureRepository.save(facture);
        
        eventPublisher.publishInvoiceFinalized(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getClientId(),
            facture.getMontantTTC()
        );
        
        return facture;
    }
    
    public Facture markInvoiceAsSent(Long invoiceId) {
        Facture facture = factureRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        facture.markAsSent();
        facture = factureRepository.save(facture);
        
        eventPublisher.publishInvoiceSent(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getClientId()
        );
        
        return facture;
    }
    
    public Facture markInvoiceAsPaid(Long invoiceId) {
        Facture facture = factureRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        facture.markAsPaid();
        facture = factureRepository.save(facture);
        
        eventPublisher.publishInvoicePaid(
            facture.getId(),
            facture.getNumeroFacture(),
            facture.getClientId(),
            facture.getMontantTTC()
        );
        
        return facture;
    }
    
    public Facture cancelInvoice(Long invoiceId, String reason) {
        Facture facture = factureRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        if (facture.getStatut() == FactureStatus.PAID) {
            throw new RuntimeException("Cannot cancel a paid invoice");
        }
        
        facture.cancel();
        facture = factureRepository.save(facture);
        
        eventPublisher.publishInvoiceCancelled(
            facture.getId(),
            facture.getNumeroFacture(),
            reason
        );
        
        return facture;
    }
    
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkOverdueInvoices() {
        log.info("Checking for overdue invoices...");
        
        List<Facture> overdueInvoices = factureRepository.findOverdueInvoices(
            FactureStatus.SENT, LocalDate.now()
        );
        
        for (Facture facture : overdueInvoices) {
            facture.markAsOverdue();
            factureRepository.save(facture);
            
            eventPublisher.publishInvoiceOverdue(
                facture.getId(),
                facture.getNumeroFacture(),
                facture.getClientId(),
                facture.getMontantTTC()
            );
            
            log.warn("Invoice {} is now overdue", facture.getNumeroFacture());
        }
        
        log.info("Found {} overdue invoices", overdueInvoices.size());
    }
    
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runMonthlyBilling() {
        log.info("Starting monthly billing run...");
        
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate periodeDebut = lastMonth.atDay(1);
        LocalDate periodeFin = lastMonth.atEndOfMonth();
        
        log.info("Monthly billing would process period {} to {}", periodeDebut, periodeFin);
    }
    
    private String generateInvoiceNumber() {
        String prefix = "FAC";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + datePart + "-" + uniquePart;
    }
    
    private InvoiceLine.LineType mapUsageType(String usageType) {
        return switch (usageType.toUpperCase()) {
            case "VOICE", "APPEL" -> InvoiceLine.LineType.USAGE_VOICE;
            case "SMS" -> InvoiceLine.LineType.USAGE_SMS;
            case "DATA", "DONNEES" -> InvoiceLine.LineType.USAGE_DATA;
            default -> InvoiceLine.LineType.USAGE_VOICE;
        };
    }
    
    private String formatUsageDescription(UsageRecordDto usage) {
        return String.format("Consommation %s - %d %s", 
            usage.type(), usage.quantite(), usage.unite());
    }
}
