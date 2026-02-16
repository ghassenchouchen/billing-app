package com.telecom.authentication.web.dto;

import com.telecom.authentication.domain.model.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        String firstName,

        String lastName,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        Role role,

        Long boutiqueId
) {}
