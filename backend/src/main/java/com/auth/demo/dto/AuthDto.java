package com.auth.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20)
        private String username;

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6)
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    // Verify OTP after signup
    @Data
    public static class VerifyOtpRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otp;
    }

    // Forgot password — step 1 (request OTP)
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    // Forgot password — step 2 (reset with OTP)
    @Data
    public static class ResetPasswordRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 6)
        private String otp;

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String message;
        private String username;
        private String email;
        private String role;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private int status;
    }
}