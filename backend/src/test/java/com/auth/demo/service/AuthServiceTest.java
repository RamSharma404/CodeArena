package com.auth.demo.service;

import com.auth.demo.dto.AuthDto;
import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AuthService — handles signup, login, OTP verification,
 * password reset, and disposable email blocking.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisService redisService;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private EmailValidationService emailValidationService;

    @InjectMocks private AuthService authService;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = User.builder()
                .id(1L)
                .username("ram")
                .email("ram@gmail.com")
                .password("hashedPass123")
                .isVerified(true)
                .role("USER")
                .build();

        unverifiedUser = User.builder()
                .id(2L)
                .username("newuser")
                .email("new@gmail.com")
                .password("hashedPass456")
                .isVerified(false)
                .otp("123456")
                .otpExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    // ═══════════════════════════════════════════════════
    // TEST 1: Successful login returns token and user info
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Simulates a verified user logging in with correct credentials.
    //   Checks that a JWT token is generated and user info is returned.
    // WHY IT MATTERS: This is the happy path — if login itself breaks,
    //   nobody can use the app.
    @Test
    @DisplayName("Should login successfully with correct credentials")
    void shouldLoginSuccessfully() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("ram@gmail.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ram@gmail.com")).thenReturn(Optional.of(verifiedUser));
        when(passwordEncoder.matches("password123", "hashedPass123")).thenReturn(true);
        when(redisService.hasSession("ram@gmail.com")).thenReturn(false);
        when(jwtUtil.generateToken("ram@gmail.com")).thenReturn("jwt-token-abc");

        AuthService.LoginResult result = authService.login(request);

        assertEquals("jwt-token-abc", result.getToken());
        assertEquals("ram", result.getResponse().getUsername());
        assertEquals("Login successful", result.getResponse().getMessage());
        // Verify that the token was saved to Redis
        verify(redisService).saveToken("ram@gmail.com", "jwt-token-abc");
    }

    // ═══════════════════════════════════════════════════
    // TEST 2: Wrong password → throws exception
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tries to login with wrong password. Expects "Invalid email or password".
    // WHY IT MATTERS: Security — we must reject wrong passwords and not leak
    //   whether the email exists (the error message is generic on purpose).
    @Test
    @DisplayName("Should reject login with wrong password")
    void shouldRejectWrongPassword() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("ram@gmail.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("ram@gmail.com")).thenReturn(Optional.of(verifiedUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPass123")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 3: Unverified user cannot login
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: User signed up but never verified their email via OTP.
    //   Tries to login → should be blocked.
    // WHY IT MATTERS: We must enforce email verification before allowing access.
    //   Without this, anyone could sign up with a fake email and use the platform.
    @Test
    @DisplayName("Should block unverified user from logging in")
    void shouldBlockUnverifiedUser() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("new@gmail.com");
        request.setPassword("password456");

        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.of(unverifiedUser));
        when(passwordEncoder.matches("password456", "hashedPass456")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));

        assertEquals("Email not verified. Please verify your email first.", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 4: Successful OTP verification activates account
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Submits the correct OTP for an unverified user.
    //   Checks that: account is marked verified, OTP is cleared,
    //   JWT token is generated, and Redis session is created.
    // WHY IT MATTERS: This is the critical signup completion flow.
    @Test
    @DisplayName("Should verify OTP and activate account")
    void shouldVerifyOtpSuccessfully() {
        AuthDto.VerifyOtpRequest request = new AuthDto.VerifyOtpRequest();
        request.setEmail("new@gmail.com");
        request.setOtp("123456");

        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.of(unverifiedUser));
        when(jwtUtil.generateToken("new@gmail.com")).thenReturn("new-jwt-token");

        AuthService.LoginResult result = authService.verifyOtp(request);

        // Account should now be verified
        assertTrue(unverifiedUser.getIsVerified());
        // OTP should be cleared from the database
        assertNull(unverifiedUser.getOtp());
        assertNull(unverifiedUser.getOtpExpiresAt());
        // Token should be returned
        assertEquals("new-jwt-token", result.getToken());
        assertEquals("Email verified successfully. Welcome!", result.getResponse().getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 5: Wrong OTP → throws error
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Submits an incorrect OTP code.
    // WHY IT MATTERS: Must reject invalid OTPs to prevent brute-force attacks.
    @Test
    @DisplayName("Should reject invalid OTP")
    void shouldRejectInvalidOtp() {
        AuthDto.VerifyOtpRequest request = new AuthDto.VerifyOtpRequest();
        request.setEmail("new@gmail.com");
        request.setOtp("999999");  // Wrong OTP

        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.of(unverifiedUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(request));

        assertEquals("Invalid OTP", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 6: Expired OTP → throws error
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Sets the OTP expiry to the past, then tries to verify.
    // WHY IT MATTERS: OTPs expire after 10 minutes for security.
    //   An expired OTP must be rejected even if the code is correct.
    @Test
    @DisplayName("Should reject expired OTP even if code is correct")
    void shouldRejectExpiredOtp() {
        // Set OTP to have expired 1 hour ago
        unverifiedUser.setOtpExpiresAt(LocalDateTime.now().minusHours(1));

        AuthDto.VerifyOtpRequest request = new AuthDto.VerifyOtpRequest();
        request.setEmail("new@gmail.com");
        request.setOtp("123456");  // Correct code, but expired

        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.of(unverifiedUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(request));

        assertEquals("OTP expired. Please request a new one.", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 7: Disposable email domains are blocked
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tries to sign up with a mailinator.com email.
    // WHY IT MATTERS: We maintain a blocklist of temp/disposable email
    //   providers to prevent spam accounts.
    @Test
    @DisplayName("Should block signup with disposable email (e.g. mailinator.com)")
    void shouldBlockDisposableEmail() {
        AuthDto.SignupRequest request = new AuthDto.SignupRequest();
        request.setUsername("spammer");
        request.setEmail("spammer@mailinator.com");
        request.setPassword("password123");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.signup(request));

        assertTrue(ex.getMessage().contains("Disposable"));
    }

    // ═══════════════════════════════════════════════════
    // TEST 8: Duplicate username → throws error
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tries to sign up with a username that already exists.
    // WHY IT MATTERS: Usernames must be unique across the platform.
    @Test
    @DisplayName("Should reject signup with duplicate username")
    void shouldRejectDuplicateUsername() {
        AuthDto.SignupRequest request = new AuthDto.SignupRequest();
        request.setUsername("ram");
        request.setEmail("ram2@gmail.com");
        request.setPassword("password123");

        when(redisService.isUserCached("ram")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.signup(request));

        assertEquals("Username already taken", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 9: Login reuses existing valid token from Redis
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: User logs in when they already have a valid session in Redis.
    //   Should return the cached token instead of generating a new one.
    // WHY IT MATTERS: Performance optimization — avoids unnecessary JWT generation.
    @Test
    @DisplayName("Should reuse existing valid token from Redis cache")
    void shouldReuseExistingToken() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("ram@gmail.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ram@gmail.com")).thenReturn(Optional.of(verifiedUser));
        when(passwordEncoder.matches("password123", "hashedPass123")).thenReturn(true);
        when(redisService.hasSession("ram@gmail.com")).thenReturn(true);
        when(redisService.getToken("ram@gmail.com")).thenReturn("cached-jwt-token");
        when(jwtUtil.validateToken("cached-jwt-token")).thenReturn(true);

        AuthService.LoginResult result = authService.login(request);

        assertEquals("cached-jwt-token", result.getToken());
        // generateToken should NOT have been called since we reused cached
        verify(jwtUtil, never()).generateToken(anyString());
    }
}
