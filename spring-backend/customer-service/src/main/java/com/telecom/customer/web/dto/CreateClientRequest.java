package com.telecom.customer.web.dto;

import java.math.BigDecimal;

public record CreateClientRequest(
    String nom,
    String prenom,
    String email,
    String telephone,
    String pieceIdentite,   // CIN / Passport number
    String adresse,
    String ville,
    String codePostal,
    String gouvernorat,
    String pays,
    String type,            // INDIVIDUAL or BUSINESS
    BigDecimal creditLimit,
    String boutiqueRef
) {}