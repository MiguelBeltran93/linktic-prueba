package com.linktic.catalog.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class ApiAuthFilter implements Filter {

    @Value("${security.api-key}")
    private String apiKey;

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();

        if (EXCLUDED_PATHS.stream().anyMatch(uri::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = httpRequest.getHeader("X-API-KEY");

        if (providedKey == null || !providedKey.equals(apiKey)) {
            log.warn("Unauthorized access attempt to URI: {}", uri);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"errors\":[{\"status\":\"401\",\"title\":\"Unauthorized\"," +
                    "\"detail\":\"Missing or invalid X-API-KEY header\"}]}"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
