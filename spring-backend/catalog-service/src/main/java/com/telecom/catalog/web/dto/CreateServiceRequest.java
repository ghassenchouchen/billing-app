package com.telecom.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateServiceRequest(
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,
    
    @NotBlank(message = "Libelle is required")
    @Size(max = 255, message = "Libelle must not exceed 255 characters")
    String libelle,
    
    @NotBlank(message = "Unite is required")
    @Size(max = 50, message = "Unite must not exceed 50 characters")
    String unite,
    
    @NotNull(message = "Prix unitaire is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Prix unitaire must be greater than 0")
    BigDecimal prixUnitaire,
    
    @NotBlank(message = "Category is required")
    String category
) {}
