package com.telecom.authentication.domain.model;

/**
 * Hierarchical roles for the telecom billing platform.
 * 
 * ADMIN > RESPONSABLE_BOUTIQUE > AGENT_COMMERCIAL
 * 
 * A higher role can perform all actions of lower roles.
 */
public enum Role {

    AGENT_COMMERCIAL(1),
    RESPONSABLE_BOUTIQUE(2),
    ADMIN(3);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this role has at least the given role's permissions.
     * Example: ADMIN.hasPermission(RESPONSABLE_BOUTIQUE) → true
     */
    public boolean hasPermission(Role required) {
        return this.level >= required.level;
    }
}
