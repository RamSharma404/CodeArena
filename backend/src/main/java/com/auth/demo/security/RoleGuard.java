package com.auth.demo.security;

import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public RoleGuard(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // Extract user from token
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
    public void requireAdmin(String authHeader) {
        System.out.println("=== ROLE CHECK ===");
        System.out.println("Auth header: " + authHeader);
        User user = getUserFromToken(authHeader);
        System.out.println("User email: " + user.getEmail());
        System.out.println("User role: " + user.getRole());
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Access denied. Admin only.");
        }
    }

    // Check if user is authenticated (any role)
    public User requireAuth(String authHeader) {
        return getUserFromToken(authHeader);
    }
}