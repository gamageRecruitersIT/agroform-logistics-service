package com.example.logistics.security;

/** Roles that can call the tracking APIs. */
public enum UserRole {
    FARMER("ROLE-008"),
    TRANSPORTER("ROLE-001"),
    DRIVER("ROLE-002");

    private final String identityRoleCode;

    UserRole(String identityRoleCode) {
        this.identityRoleCode = identityRoleCode;
    }

    public boolean matchesIdentityRoleCode(String roleCode) {
        return name().equalsIgnoreCase(roleCode)
                || identityRoleCode.equalsIgnoreCase(roleCode);
    }
}
