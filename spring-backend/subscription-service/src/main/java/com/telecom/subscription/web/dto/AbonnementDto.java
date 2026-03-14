package com.telecom.subscription.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AbonnementDto(
    Long id,
    Long clientId,
    String clientRef,
    Long offreId,
    LocalDate dateDebut,
    LocalDate dateFin,
    String status,
    String billingFrequency,
    LocalDateTime createdAt
) {}
