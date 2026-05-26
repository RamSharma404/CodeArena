package com.auth.demo.controller;

import com.auth.demo.dto.ProfileStatsDto;
import com.auth.demo.dto.UserProfileUpdateDto;
import com.auth.demo.model.User;
import com.auth.demo.security.RoleGuard;
import com.auth.demo.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class UserController {

    private final ProfileService profileService;
    private final RoleGuard roleGuard;

    public UserController(ProfileService profileService, RoleGuard roleGuard) {
        this.profileService = profileService;
        this.roleGuard = roleGuard;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try {
            User user = roleGuard.requireAuth(request);
            ProfileStatsDto stats = profileService.getUserProfileStats(user.getId());
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(HttpServletRequest request, @RequestBody UserProfileUpdateDto updateDto) {
        try {
            User user = roleGuard.requireAuth(request);
            profileService.updateUserProfile(user.getId(), updateDto);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
