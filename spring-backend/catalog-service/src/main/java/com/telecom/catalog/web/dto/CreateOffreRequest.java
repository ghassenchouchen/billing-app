package com.telecom.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateOffreRequest(
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,
    
    @NotBlank(message = "Libelle is required")
    @Size(max = 255, message = "Libelle must not exceed 255 characters")
    String libelle,
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,
    
    @NotNull(message = "Prix mensuel is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Prix mensuel must be greater than 0")
    BigDecimal prixMensuel,
    
    @NotNull(message = "Date debut is required")
    LocalDate dateDebut,
    
    LocalDate dateFin,
    
    @NotBlank(message = "Payment type is required")
    String paymentType,
    
    List<Long> serviceIds
) {}
