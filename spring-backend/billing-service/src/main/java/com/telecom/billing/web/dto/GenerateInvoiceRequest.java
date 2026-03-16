package com.telecom.billing.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateInvoiceRequest(
    @NotNull(message = "Subscription ID is required")
    Long abonnementId,
    
    @NotNull(message = "Period start date is required")
    LocalDate periodeDebut,
    
    @NotNull(message = "Period end date is required")
    LocalDate periodeFin
) {}
