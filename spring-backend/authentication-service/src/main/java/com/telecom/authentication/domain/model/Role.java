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

     
    public boolean hasPermission(Role required) {
        return this.level >= required.level;
    }
}
