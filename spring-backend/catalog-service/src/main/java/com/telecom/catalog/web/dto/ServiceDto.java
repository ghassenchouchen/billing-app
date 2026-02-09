package com.telecom.catalog.web.dto;

import java.math.BigDecimal;

public record ServiceDto(
    Long id,
    String code,
    String libelle,
    String unite,
    BigDecimal prixUnitaire,
    String category,
    boolean active
) {}
