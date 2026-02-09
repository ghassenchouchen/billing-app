package com.telecom.catalog.web.dto;

import java.math.BigDecimal;

public record CreateServiceRequest(
    String code,
    String libelle,
    String unite,
    BigDecimal prixUnitaire,
    String category
) {}
