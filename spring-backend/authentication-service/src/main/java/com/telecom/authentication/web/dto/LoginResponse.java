package com.telecom.authentication.web.dto;

public record LoginResponse(
        boolean success,
        String token,
        String refreshToken,
        String role,
        String username,
        String firstName,
        String lastName,
        Long boutiqueId,
        String message
) {
    public static LoginResponse success(String token, String refreshToken, String role,
                                        String username, String firstName, String lastName,
                                        Long boutiqueId) {
        return new LoginResponse(true, token, refreshToken, role, username, firstName, lastName, boutiqueId, null);
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, null, null, null, null, null, null, null, message);
    }
}
