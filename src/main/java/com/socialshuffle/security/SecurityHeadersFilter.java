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


        // 2. MIME type sniffing prevention
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 3. XSS Filter enable in supporting browsers
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 4. Strict Referrer Policy: Never leak query parameters or private paths to external domains
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 5. Restrict device hardware permissions
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");

        // 6. Cross-Origin Resource Sharing (CORS) Headers
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        }
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, Range, X-CSRF-Token, Cache-Control, Pragma, If-Modified-Since, X-Api-Key");
        httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Length, Content-Range, Content-Disposition, X-Total-Count, ETag");
        httpResponse.setHeader("Access-Control-Max-Age", "86400");

        // Handle pre-flight OPTIONS request cleanly
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
