package com.telecom.boutique.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddSimRequest(
    @NotBlank(message = "L'ICCID est obligatoire")
    @Size(min = 18, max = 22, message = "L'ICCID doit contenir entre 18 et 22 caractères")
    String iccid,

    @Size(max = 20, message = "L'IMSI ne doit pas dépasser 20 caractères")
    String imsi,

    @Size(max = 20, message = "Le MSISDN ne doit pas dépasser 20 caractères")
    String msisdn,

    @NotBlank(message = "Le type de SIM est obligatoire")
    @Pattern(regexp = "STANDARD|ESIM", message = "Le type doit être STANDARD ou ESIM")
    String simType
) {}
