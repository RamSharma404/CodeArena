package com.auth.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Send OTP for email verification
    public void sendVerificationOtp(String toEmail, String otp) {
        System.out.println("\n==============================================");
        System.out.println("🚀 DEV/RENDER MODE: OTP for " + toEmail + " is: " + otp);
        System.out.println("==============================================\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Email Verification OTP");
        message.setText(
                "Hello,\n\n" +
                        "Your OTP for email verification is: " + otp + "\n\n" +
                        "This OTP expires in 10 minutes.\n\n" +
                        "If you did not request this, ignore this email."
        );
        
        new Thread(() -> {
            try {
                mailSender.send(message);
            } catch (Exception e) {
                System.out.println("SMTP blocked or failed (expected on Render Free): " + e.getMessage());
            }
        }).start();
    }

    // Send OTP for password reset
    public void sendPasswordResetOtp(String toEmail, String otp) {
        System.out.println("\n==============================================");
        System.out.println("🚀 DEV/RENDER MODE: Reset OTP for " + toEmail + " is: " + otp);
        System.out.println("==============================================\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset OTP");
        message.setText(
                "Hello,\n\n" +
                        "Your OTP for password reset is: " + otp + "\n\n" +
                        "This OTP expires in 10 minutes.\n\n" +
                        "If you did not request this, please secure your account immediately."
        );
        
        new Thread(() -> {
            try {
                mailSender.send(message);
            } catch (Exception e) {
                System.out.println("SMTP blocked or failed (expected on Render Free): " + e.getMessage());
            }
        }).start();
    }
}