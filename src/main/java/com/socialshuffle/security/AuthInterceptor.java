package com.socialshuffle.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SecurityTokenService tokenService;

    public AuthInterceptor(SecurityTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Allow OPTIONS preflight checks
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Only enforce token check on admin and export paths
        if (path.startsWith("/api/admin/") || path.startsWith("/api/export/")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
                return false;
            }

            String token = authHeader.substring(7).trim();
            if (!tokenService.validateToken(token)) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return false;
            }

            String role = tokenService.extractRole(token);
            if (!"admin".equalsIgnoreCase(role)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied: Administrator privileges required");
                return false;
            }
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"error\": \"" + message + "\", \"status\": " + status + "}");
        writer.flush();
    }
}
