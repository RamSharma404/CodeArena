package com.auth.demo.service;

import com.auth.demo.dto.AuthDto;
import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
@Service
public class AuthService {
    // Known disposable/temp email domains
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "mailinator.com", "guerrillamail.com", "tempmail.com",
            "throwaway.email", "yopmail.com", "sharklasers.com",
            "guerrillamailblock.com", "grr.la", "guerrillamail.info",
            "spam4.me", "trashmail.com", "dispostable.com",
            "maildrop.cc", "mailnull.com", "spamgourmet.com",
            "trashmail.at", "trashmail.io", "trashmail.me",
            "discard.email", "fakeinbox.com", "tempinbox.com",
            "getairmail.com", "filzmail.com", "throwam.com",
            "spamfree24.org", "mailexpire.com", "spammotel.com",
            "10minutemail.com", "10minutemail.net", "10minemail.com",
            "20minutemail.com", "temp-mail.org", "tempmail.net",
            "getnada.com", "mailtemp.info", "0-mail.com",
            "0815.ru", "0clickemail.com", "mailnesia.com"
    );
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final EmailValidationService emailValidationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, RedisService redisService,
                       EmailService emailService, OtpService otpService,
                       EmailValidationService emailValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
        this.emailService = emailService;
        this.otpService = otpService;
        this.emailValidationService = emailValidationService;
    }

    // ─── SIGNUP ──────────────────────────────────────────
    // Saves user as unverified → sends OTP to email
    public AuthDto.MessageResponse signup(AuthDto.SignupRequest request) {
        validateEmailDomain(request.getEmail());

        // 2. Block disposable emails via API (catches new domains)
        emailValidationService.validateEmail(request.getEmail());
        if (redisService.isUserCached(request.getUsername()))
            throw new RuntimeException("Username already taken");

        if (userRepository.existsByUsername(request.getUsername())) {
            redisService.markUserExists(request.getUsername());
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already registered");

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        String otp = otpService.generateOtp();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(hashedPassword)
                .isVerified(false)
                .otp(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        userRepository.save(user);

        // Send OTP to email
        emailService.sendVerificationOtp(request.getEmail(), otp);

        return new AuthDto.MessageResponse(
                "Signup successful. OTP sent to " + request.getEmail() + ". Verify to activate account."
        );
    }

    // ─── VERIFY OTP ──────────────────────────────────────
    // User enters OTP → account activated → JWT returned via cookie
    public LoginResult verifyOtp(AuthDto.VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified())
            throw new RuntimeException("Account already verified");

        if (!user.getOtp().equals(request.getOtp()))
            throw new RuntimeException("Invalid OTP");

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new one.");

        // Mark as verified and clear OTP
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        redisService.markUserExists(user.getUsername());
        String token = jwtUtil.generateToken(user.getEmail());
        redisService.saveToken(user.getEmail(), token);

        AuthDto.AuthResponse response = new AuthDto.AuthResponse(
                "Email verified successfully. Welcome!",
                user.getUsername(), user.getEmail(), user.getRole());
        return new LoginResult(token, response);
    }

    // ─── RESEND OTP ──────────────────────────────────────
    public AuthDto.MessageResponse resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified())
            throw new RuntimeException("Account already verified");

        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendVerificationOtp(email, otp);
        return new AuthDto.MessageResponse("New OTP sent to " + email);
    }

    // ─── LOGIN ───────────────────────────────────────────
    public LoginResult login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid email or password");

        // Block unverified users from logging in
        if (!user.getIsVerified())
            throw new RuntimeException("Email not verified. Please verify your email first.");

        String token;
        if (redisService.hasSession(request.getEmail())) {
            String cachedToken = redisService.getToken(request.getEmail());
            if (cachedToken != null && jwtUtil.validateToken(cachedToken)) {
                token = cachedToken;
            } else {
                token = jwtUtil.generateToken(user.getEmail());
                redisService.saveToken(user.getEmail(), token);
            }
        } else {
            token = jwtUtil.generateToken(user.getEmail());
            redisService.saveToken(user.getEmail(), token);
        }

        AuthDto.AuthResponse response = new AuthDto.AuthResponse(
                "Login successful",
                user.getUsername(), user.getEmail(), user.getRole());
        return new LoginResult(token, response);
    }

    // ─── FORGOT PASSWORD — Step 1 ────────────────────────
    // User requests OTP to reset password
    public AuthDto.MessageResponse forgotPassword(AuthDto.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendPasswordResetOtp(request.getEmail(), otp);
        return new AuthDto.MessageResponse("Password reset OTP sent to " + request.getEmail());
    }

    // ─── FORGOT PASSWORD — Step 2 ────────────────────────
    // User enters OTP + new password
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null)
            throw new RuntimeException("No OTP requested. Please request a new one.");

        if (!user.getOtp().equals(request.getOtp()))
            throw new RuntimeException("Invalid OTP");

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new one.");

        // Update password and clear OTP
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        // Clear Redis session so they must login again
        redisService.deleteSession(request.getEmail());

        return new AuthDto.MessageResponse("Password reset successful. Please login with your new password.");
    }
    private void validateEmailDomain(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (BLOCKED_DOMAINS.contains(domain)) {
            throw new RuntimeException("Disposable/temporary email addresses are not allowed. Please use a real email.");
        }
    }
    // ─── LOGOUT ──────────────────────────────────────────
    public void logout(String email) {
        redisService.deleteSession(email);
    }

    // ─── Wrapper: holds token (for cookie) + response (for body) ───
    @Getter
    @AllArgsConstructor
    public static class LoginResult {
        private final String token;
        private final AuthDto.AuthResponse response;
    }
}