package com.corplearning.common;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    EMPLOYEE,
    TRAINER,
    LINE_MANAGER,
    LND_ADMIN,
    COMPLIANCE_OFFICER,
    INTEGRATOR;

    public static UserRole fromHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }

    public boolean has(Permission permission) {
        return RolePermissions.forRole(this).contains(permission);
    }
}

enum Permission {
    RULE_CONFIGURE,
    REPORT_GENERATE,
    REPORT_EXPORT,
    REPORT_VIEW,
    AUDIT_VIEW
}

final class RolePermissions {

    private RolePermissions() {}

    static Set<Permission> forRole(UserRole role) {
        switch (role) {
            case LND_ADMIN:
                return EnumSet.of(Permission.RULE_CONFIGURE, Permission.AUDIT_VIEW);
            case COMPLIANCE_OFFICER:
                return EnumSet.of(
                        Permission.REPORT_GENERATE,
                        Permission.REPORT_EXPORT,
                        Permission.REPORT_VIEW,
                        Permission.AUDIT_VIEW);
            case TRAINER:
            case LINE_MANAGER:
            case EMPLOYEE:
            case INTEGRATOR:
            default:
                return EnumSet.noneOf(Permission.class);
        }
    }
}
