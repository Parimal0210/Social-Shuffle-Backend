package com.socialshuffle.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "authUserId";
    public static final String ATTR_ROLE = "authRole";
    public static final String ATTR_TOKEN = "authToken";

    private final SecurityTokenService tokenService;

    public AuthInterceptor(SecurityTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        String userId = null;
        String role = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (tokenService.validateToken(token)) {
                userId = tokenService.extractUserId(token);
                role = tokenService.extractRole(token);
                request.setAttribute(ATTR_USER_ID, userId);
                request.setAttribute(ATTR_ROLE, role);
                request.setAttribute(ATTR_TOKEN, token);
            }
        }

        boolean isAuthenticated = (userId != null && role != null);
        boolean isAdmin = "admin".equalsIgnoreCase(role);

        // 1. Strict Admin-only routes (method-aware)
        if (requiresAdmin(path, method)) {
            if (!isAuthenticated) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return false;
            }
            if (!isAdmin) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied: Administrator privileges required");
                return false;
            }
            return true;
        }

        // 2. Authenticated routes (admin OR participant)
        if (requiresAuth(path, method)) {
            if (!isAuthenticated) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
                return false;
            }
            return true;
        }

        return true;
    }

    private boolean requiresAdmin(String path, String method) {
        if (path.startsWith("/api/admin/") || path.startsWith("/api/export/")) return true;
        if (path.startsWith("/api/communications/")) return true;
        if (path.startsWith("/api/audit-logs")) return true;

        if ("/api/community/stats".equals(path) && !"GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/community/reports") && !"POST".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/community/volunteers") && "PATCH".equalsIgnoreCase(method)) return true;

        if (path.startsWith("/api/events") && !"GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/games") && !"GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/venues") && !"GET".equalsIgnoreCase(method)) return true;

        if (path.startsWith("/api/participants")) {
            if ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) return true;
            if (path.startsWith("/api/participants/check-duplicate") || path.startsWith("/api/participants/merge")) return true;
        }

        if (path.startsWith("/api/registrations/walk-in")
                || path.startsWith("/api/registrations/bulk-import")
                || path.startsWith("/api/registrations/verify-qr")
                || path.startsWith("/api/registrations/revert-priya-35")) {
            return true;
        }
        if (path.startsWith("/api/registrations") && path.contains("/payment")) return true;

        if (path.startsWith("/api/notifications/event-broadcast")
                || path.startsWith("/api/notifications/events/")
                || path.startsWith("/api/notifications/push-notification/")
                || path.startsWith("/api/notifications/send-reminder/")
                || path.startsWith("/api/notifications/community-announcement")) {
            return true;
        }

        if ("/api/payments/razorpay/config".equals(path) && !"GET".equalsIgnoreCase(method)) return true;

        return false;
    }

    private boolean requiresAuth(String path, String method) {
        if (path.startsWith("/api/participants")) return true;
        if (path.startsWith("/api/registrations")) return true;
        if ("/api/community/volunteers".equals(path) && "GET".equalsIgnoreCase(method)) return true;
        if (path.startsWith("/api/notifications") && !"GET".equalsIgnoreCase(method)) return true;
        if ("/api/payments/razorpay/create-order".equals(path) || "/api/payments/razorpay/verify-payment".equals(path)) return true;
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"success\":false,\"error\":\"" + message + "\",\"message\":\"" + message + "\",\"status\":" + status + "}");
        writer.flush();
    }
}
