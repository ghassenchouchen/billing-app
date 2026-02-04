package com.telecom.customer.web.dto;

import java.time.LocalDateTime;

public record ClientDto(
    Long id,
    String nom,
    String prenom,
    String email,
    String telephone,
    String adresse,
    String type,
    String status,
    LocalDateTime createdAt
) {}
