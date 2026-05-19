package com.telecom.customer.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateClientRequest(
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    String nom,

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    String prenom,

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    String email,

    @Pattern(regexp = "^\\d{8}$", message = "Le numéro de téléphone doit contenir exactement 8 chiffres")
    String telephone,

    @Pattern(regexp = "^\\d{8}$", message = "La pièce d'identité (CIN) doit contenir exactement 8 chiffres")
    String pieceIdentite,

    String adresse,
    String ville,
    String codePostal,
    String gouvernorat,
    String pays,

    @NotBlank(message = "Le type de client est obligatoire")
    @Pattern(regexp = "INDIVIDUAL|BUSINESS", message = "Le type doit être INDIVIDUAL ou BUSINESS")
    String type,

    BigDecimal creditLimit,
    String boutiqueRef
) {}