package com.telecom.boutique.web.dto;

import jakarta.validation.constraints.Size;


public record UpdateBoutiqueRequest(
    @Size(max = 100) String nom,
    @Size(max = 255) String adresse,
    @Size(max = 100) String ville,
    @Size(max = 10) String codePostal,
    String telephone,
    String email,
    Long responsableId,
    String status
) {}
