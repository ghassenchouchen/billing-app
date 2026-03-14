package com.telecom.boutique.web.dto;

import jakarta.validation.constraints.*;

public record CreateBoutiqueRequest(
    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères")
    String code,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    String nom,

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    String adresse,

    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
    String ville,

    @Size(max = 10, message = "Le code postal ne doit pas dépasser 10 caractères")
    String codePostal,

    @Pattern(regexp = "^[+]?[0-9\\s-]{6,20}$", message = "Numéro de téléphone invalide")
    String telephone,

    @Email(message = "Adresse email invalide")
    String email,

    Long responsableId
) {}
