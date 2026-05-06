package com.auth.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendVerificationOtp(String toEmail, String otp) {
        logOtp("OTP", toEmail, otp);

        String subject = "CodeArena - Email Verification OTP";
        String htmlContent = "<p>Hello,</p><p>Your OTP for email verification is: <strong>" + otp + "</strong></p><p>This OTP expires in 10 minutes.</p>";
        
        sendViaBrevo(toEmail, subject, htmlContent);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        logOtp("Reset OTP", toEmail, otp);

        String subject = "CodeArena - Password Reset OTP";
        String htmlContent = "<p>Hello,</p><p>Your OTP for password reset is: <strong>" + otp + "</strong></p><p>This OTP expires in 10 minutes.</p>";
        
        sendViaBrevo(toEmail, subject, htmlContent);
    }

    private void logOtp(String type, String toEmail, String otp) {
        System.out.println("\n==============================================");
        System.out.println("🚀 DEV/RENDER MODE: " + type + " for " + toEmail + " is: " + otp);
        System.out.println("==============================================\n");
    }

    private void sendViaBrevo(String toEmail, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            System.out.println("Brevo API Key not set. Email not sent.");
            return;
        }

        new Thread(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("sender", Map.of("name", "CodeArena", "email", senderEmail));
                body.put("to", List.of(Map.of("email", toEmail)));
                body.put("subject", subject);
                body.put("htmlContent", htmlContent);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
                System.out.println("✅ Email sent successfully via Brevo API");
            } catch (Exception e) {
                System.out.println("❌ Failed to send email via Brevo API: " + e.getMessage());
            }
        }).start();
    }
}