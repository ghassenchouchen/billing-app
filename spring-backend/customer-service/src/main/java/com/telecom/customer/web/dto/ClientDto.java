package com.telecom.customer.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientDto(
     Long id,                    // Internal 
    String customerRef,         // used for APIs
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
