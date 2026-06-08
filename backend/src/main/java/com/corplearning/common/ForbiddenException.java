package com.corplearning.common;

public class ForbiddenException extends RuntimeException {

    private final String permission;
    private final UserRole role;

    public ForbiddenException(String permission, UserRole role) {
        super("Role " + role + " lacks permission " + permission);
        this.permission = permission;
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public UserRole getRole() {
        return role;
    }
}
