package com.telecom.catalog.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateOffreRequest(
    String code,
    String libelle,
    String description,
    BigDecimal prixMensuel,
    LocalDate dateDebut,
    LocalDate dateFin,
    String paymentType,
    List<Long> serviceIds
) {}
