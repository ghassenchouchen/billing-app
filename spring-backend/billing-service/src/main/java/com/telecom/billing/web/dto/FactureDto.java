package com.telecom.billing.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FactureDto(
    Long id,
    String numeroFacture,
    Long clientId,
    Long abonnementId,
    LocalDate dateFacture,
    LocalDate dateEcheance,
    LocalDate periodeDebut,
    LocalDate periodeFin,
    BigDecimal montantHT,
    BigDecimal montantTVA,
    BigDecimal montantTTC,
    String statut,
    int nombreLignes,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    List<InvoiceLineDto> lines
) {
    public record InvoiceLineDto(
        Long id,
        String type,
        String description,
        Long serviceId,
        Long usageId,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal montant
    ) {}
}
