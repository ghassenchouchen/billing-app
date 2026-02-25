package com.telecom.customer.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record ClientDto(
    String customerRef,         // public identifier 
    String nom,
    String prenom,
    String email,
    String telephone,
    String pieceIdentite,       // CIN/Passport number
    String adresse,
    String ville,
    String codePostal,
    String pays,
    String type,
    String status,
    BigDecimal accountBalance,
    BigDecimal creditLimit,
    LocalDateTime createdAt
) {}
