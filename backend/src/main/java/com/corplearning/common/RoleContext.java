package com.corplearning.common;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public final class RoleContext {

    public static final String ROLE_HEADER = "X-User-Role";

    private RoleContext() {}

    public static UserRole currentRole() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return UserRole.LND_ADMIN;
        }
        Object attr = request.getAttribute(RoleAuthFilter.ROLE_ATTRIBUTE);
        if (attr instanceof UserRole) {
            return (UserRole) attr;
        }
        return UserRole.LND_ADMIN;
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }
}
