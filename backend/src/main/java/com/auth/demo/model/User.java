package com.auth.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(name = "provider", length = 20)
    private String provider = "local";

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String bio;

    @Column(name = "photo_url", columnDefinition = "LONGTEXT")
    private String photoUrl;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    // ADMIN or USER
    @Column(name = "role", length = 20)
    private String role = "USER";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = "USER";
    }
}