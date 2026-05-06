package com.auth.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${emailjs.service.id:}")
    private String serviceId;

    @Value("${emailjs.template.id:}")
    private String templateId;

    @Value("${emailjs.public.key:}")
    private String publicKey;

    @Value("${emailjs.private.key:}")
    private String privateKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendVerificationOtp(String toEmail, String otp) {
        logOtp("OTP", toEmail, otp);
        sendViaEmailJs(toEmail, otp);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        logOtp("Reset OTP", toEmail, otp);
        sendViaEmailJs(toEmail, otp);
    }

    private void logOtp(String type, String toEmail, String otp) {
        System.out.println("\n==============================================");
        System.out.println("🚀 DEV/RENDER MODE: " + type + " for " + toEmail + " is: " + otp);
        System.out.println("==============================================\n");
    }

    private void sendViaEmailJs(String toEmail, String otp) {
        if (serviceId == null || serviceId.isEmpty()) {
            System.out.println("EmailJS keys not set. Email not sent.");
            return;
        }

        new Thread(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("to_email", toEmail);
                templateParams.put("passcode", otp);
                templateParams.put("time", "15 minutes");

                Map<String, Object> body = new HashMap<>();
                body.put("service_id", serviceId);
                body.put("template_id", templateId);
                body.put("user_id", publicKey);
                body.put("accessToken", privateKey);
                body.put("template_params", templateParams);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                restTemplate.postForEntity("https://api.emailjs.com/api/v1.0/email/send", request, String.class);
                System.out.println("✅ Email sent successfully via EmailJS API");
            } catch (Exception e) {
                System.out.println("❌ Failed to send email via EmailJS API: " + e.getMessage());
            }
        }).start();
    }
}