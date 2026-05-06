package com.auth.demo.security;

import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public RoleGuard(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // Extract JWT token from request: cookie first, then Authorization header fallback
    private String extractToken(HttpServletRequest request) {
        // 1. Try HttpOnly cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Fallback: Authorization header (for API clients, Swagger, etc.)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "");
        }

        return null;
    }

    // Extract user from request (cookie or header)
    public User getUserFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null)
            throw new RuntimeException("No token provided");

        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid or expired token");

        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Legacy method: extract user from Authorization header string
    public User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new RuntimeException("No token provided");

        String token = authHeader.replace("Bearer ", "");

        if (!jwtUtil.validateToken(token))
            throw new RuntimeException("Invalid or expired token");

        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Check if user is admin
    public void requireAdmin(HttpServletRequest request) {
        System.out.println("=== ROLE CHECK ===");
        User user = getUserFromRequest(request);
        System.out.println("User email: " + user.getEmail());
        System.out.println("User role: " + user.getRole());
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Access denied. Admin only.");
        }
    }

    // Check if user is authenticated (any role)
    public User requireAuth(HttpServletRequest request) {
        return getUserFromRequest(request);
    }
}