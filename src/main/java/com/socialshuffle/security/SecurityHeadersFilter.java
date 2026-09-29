package com.socialshuffle.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. Anti-Clickjacking / Anti-Phishing: Prevent malicious sites from embedding the app in an invisible iframe
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");

        // 2. MIME type sniffing prevention
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 3. XSS Filter enable in supporting browsers
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 4. Strict Referrer Policy: Never leak query parameters or private paths to external domains
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 5. Restrict device hardware permissions
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");

        // 6. Content Security Policy for API layer
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");

        // Handle pre-flight OPTIONS request cleanly
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
