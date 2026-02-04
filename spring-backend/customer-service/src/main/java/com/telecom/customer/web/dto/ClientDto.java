package com.telecom.customer.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DESIGN: Only exposes customerRef (UUID) - never exposes internal database id
 * SECURITY: Prevents enumeration attacks and exposure of internal structure
 * PUBLIC: This is the contract for external API consumers
 */
public record ClientDto(
    String customerRef,         // PUBLIC identifier - use this for all external references
    String nom,
    String prenom,
    String email,
    String telephone,
    String adresse,
    String ville,
    String codePostal,
    String pays,
    String type,
    String status,
    String paymentType,
    BigDecimal accountBalance,
    BigDecimal creditLimit,
    LocalDateTime createdAt
) {}
