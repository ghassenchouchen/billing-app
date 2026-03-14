package com.telecom.boutique.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignSimRequest(
    @NotNull(message = "L'ID client est obligatoire")
    Long clientId
) {}
