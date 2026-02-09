package com.telecom.customer.web.dto;

import java.math.BigDecimal;

public record CreateClientRequest(
    String nom,
    String prenom,
    String email,
    String telephone,
    String adresse,
    String ville,
    String codePostal,
    String pays,
    String type,            // SIMPLE or ENTREPRISE
    String paymentType,     // PREPAID or POSTPAID
    BigDecimal creditLimit  // Optional, defaults to 500
) {}