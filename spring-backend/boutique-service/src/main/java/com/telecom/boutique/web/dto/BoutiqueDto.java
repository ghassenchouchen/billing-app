package com.telecom.boutique.web.dto;

import java.time.LocalDateTime;

public record BoutiqueDto(
    Long id,
    String code,
    String nom,
    String adresse,
    String ville,
    String codePostal,
    String telephone,
    String email,
    Long responsableId,
    String status,
    LocalDateTime createdAt
) {}
