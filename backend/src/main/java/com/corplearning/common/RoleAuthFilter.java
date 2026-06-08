package com.corplearning.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RoleAuthFilter implements Filter {

    public static final String ROLE_ATTRIBUTE = "corp.userRole";

    @Value("${corp-learning.default-role:LND_ADMIN}")
    private String defaultRole;

    private final ObjectMapper objectMapper;

    public RoleAuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        UserRole role = resolveRole(req);
        req.setAttribute(ROLE_ATTRIBUTE, role);

        Optional<Permission> required = requiredPermission(req.getMethod(), req.getRequestURI());
        if (required.isPresent() && !role.has(required.get())) {
            writeForbidden((HttpServletResponse) response, required.get(), role);
            return;
        }
        chain.doFilter(request, response);
    }

    private UserRole resolveRole(HttpServletRequest req) {
        String header = req.getHeader(RoleContext.ROLE_HEADER);
        UserRole parsed = UserRole.fromHeader(header);
        if (parsed != null) {
            return parsed;
        }
        try {
            return UserRole.valueOf(defaultRole.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.LND_ADMIN;
        }
    }

    private Optional<Permission> requiredPermission(String method, String uri) {
        if (uri.startsWith("/api/v1/ingest")
                || uri.startsWith("/actuator")
                || uri.startsWith("/h2-console")
                || uri.startsWith("/swagger")
                || uri.equals("/")
                || uri.endsWith(".html")
                || uri.endsWith(".css")
                || uri.endsWith(".js")) {
            return Optional.empty();
        }
        if ("POST".equals(method) && "/api/v1/rules".equals(uri)) {
            return Optional.of(Permission.RULE_CONFIGURE);
        }
        if ("POST".equals(method) && uri.matches("/api/v1/rules/\\d+/activate")) {
            return Optional.of(Permission.RULE_CONFIGURE);
        }
        if ("POST".equals(method) && "/api/v1/rules/dry-run".equals(uri)) {
            return Optional.of(Permission.RULE_CONFIGURE);
        }
        if ("POST".equals(method) && ("/api/v1/reports/compliance".equals(uri)
                || "/api/v1/reports/corporate".equals(uri))) {
            return Optional.of(Permission.REPORT_GENERATE);
        }
        if ("GET".equals(method) && uri.matches("/api/v1/reports/\\d+/export.*")) {
            return Optional.of(Permission.REPORT_EXPORT);
        }
        if ("GET".equals(method) && ("/api/v1/reports".equals(uri)
                || uri.matches("/api/v1/reports/\\d+"))) {
            return Optional.of(Permission.REPORT_VIEW);
        }
        if ("GET".equals(method) && uri.startsWith("/api/v1/audit/")) {
            return Optional.of(Permission.AUDIT_VIEW);
        }
        return Optional.empty();
    }

    private void writeForbidden(HttpServletResponse res, Permission permission, UserRole role)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Forbidden");
        body.put("requiredPermission", permission.name());
        body.put("role", role.name());
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
