package com.telecom.authentication.web.dto;

import com.telecom.authentication.domain.model.Role;
import com.telecom.authentication.domain.model.User;
import com.telecom.authentication.domain.model.UserStatus;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String firstName,
        String lastName,
        Role role,
        UserStatus status,
        Long boutiqueId,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                user.getBoutiqueId(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
