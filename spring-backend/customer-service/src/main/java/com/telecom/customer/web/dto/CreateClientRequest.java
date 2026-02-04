package com.telecom.customer.web.dto;

public record CreateClientRequest(
    String nom,
    String prenom,
    String email,
    String telephone,
    String adresse,
    String type
) {}
