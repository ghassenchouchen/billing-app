package com.telecom.boutique.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
    Long id,
    String reference,
    Long boutiqueId,
    Long agentId,
    Long clientId,
    String clientNom,
    String offreLibelle,
    String typeTransaction,
    BigDecimal montant,
    String status,
    LocalDateTime createdAt
) {}
