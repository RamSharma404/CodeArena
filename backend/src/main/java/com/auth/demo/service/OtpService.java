package com.auth.demo.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();

    // Generate a 6 digit OTP
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}