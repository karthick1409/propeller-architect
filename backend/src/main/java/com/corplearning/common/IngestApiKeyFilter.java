package com.corplearning.common;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class IngestApiKeyFilter implements Filter {

    @Value("${corp-learning.ingest-api-key}")
    private String expectedKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().startsWith("/api/v1/ingest")) {
            String key = req.getHeader("X-API-Key");
            if (key == null || !key.equals(expectedKey)) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("{\"error\":\"Invalid or missing X-API-Key\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
