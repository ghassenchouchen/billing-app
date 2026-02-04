package com.telecom.customer.web.dto;

public record LoginResponse(String token, String email, String role, Long clientId) {}
