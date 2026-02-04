package com.telecom.customer.web.dto;

public record RegisterRequest(String email, String password, String role, Long clientId) {}
