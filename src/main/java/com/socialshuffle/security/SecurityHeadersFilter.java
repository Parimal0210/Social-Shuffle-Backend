package com.socialshuffle.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://socialshufflepune.com,https://www.socialshufflepune.com,https://socialshuffle.in,https://www.socialshuffle.in}")
    private String allowedOrigins;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");

        String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");
        if (httpRequest.isSecure() || "https".equalsIgnoreCase(forwardedProto)) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String uri = httpRequest.getRequestURI();
        if (uri != null && (uri.startsWith("/api/auth/") || uri.startsWith("/api/communications/"))) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            httpResponse.setHeader("Pragma", "no-cache");
        }

        String origin = httpRequest.getHeader("Origin");
        if (origin != null && !origin.isEmpty() && isAllowedOrigin(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Vary", "Origin");
        }
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Range, X-CSRF-Token, Cache-Control, Pragma, If-Modified-Since, X-Api-Key");
        httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Length, Content-Range, Content-Disposition, X-Total-Count, ETag");
        httpResponse.setHeader("Access-Control-Max-Age", "86400");

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        if ("*".equals(allowedOrigins.trim())) {
            return true;
        }
        Set<String> configured = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (configured.contains(origin)) {
            return true;
        }
        try {
            String host = URI.create(origin).getHost();
            if (host == null) return false;
            return "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || host.endsWith(".run.app")
                    || host.endsWith(".socialshufflepune.com")
                    || host.equalsIgnoreCase("socialshufflepune.com")
                    || host.endsWith(".socialshuffle.in")
                    || host.equalsIgnoreCase("socialshuffle.in");
        } catch (Exception e) {
            return false;
        }
    }
}
