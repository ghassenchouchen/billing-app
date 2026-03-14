package com.telecom.customer.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateClientRequest(
    @NotBlank(message = "Nom is required")
    @Size(max = 100, message = "Nom must not exceed 100 characters")
    String nom,
    
    @NotBlank(message = "Prenom is required")
    @Size(max = 100, message = "Prenom must not exceed 100 characters")
    String prenom,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    String email,
    
    @NotBlank(message = "Telephone is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Telephone must be a valid phone number")
    String telephone,
    
    @NotBlank(message = "Piece identite is required")
    @Size(min = 5, max = 50, message = "Piece identite must be between 5 and 50 characters")
    String pieceIdentite,   // CIN / Passport number
    
    @Size(max = 255, message = "Adresse must not exceed 255 characters")
    String adresse,
    
    @Size(max = 100, message = "Ville must not exceed 100 characters")
    String ville,
    
    @Size(max = 20, message = "Code postal must not exceed 20 characters")
    String codePostal,
    
    @Size(max = 100, message = "Pays must not exceed 100 characters")
    String pays,
    
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(INDIVIDUAL|BUSINESS)$", message = "Type must be INDIVIDUAL or BUSINESS")
    String type,            // INDIVIDUAL or BUSINESS
    
    @DecimalMin(value = "0.0", message = "Credit limit must be non-negative")
    BigDecimal creditLimit  
) {}