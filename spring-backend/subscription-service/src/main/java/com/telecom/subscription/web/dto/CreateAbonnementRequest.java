package com.telecom.subscription.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import com.telecom.subscription.domain.entity.BillingFrequency;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateAbonnementRequest(
    Long clientId,
    String clientRef,
    Long offreId,
    LocalDate dateDebut,
    LocalDate dateFin,
    BillingFrequency billingFrequency
) {}
