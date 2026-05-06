package com.auth.demo.controller;

import com.auth.demo.dto.AuthDto;
import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import com.auth.demo.security.JwtUtil;
import com.auth.demo.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // ─── Helper: create JWT HttpOnly cookie ───────────────
    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ─── Helper: clear JWT cookie ─────────────────────────
    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // POST /api/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody AuthDto.SignupRequest request) {
        try {
            return ResponseEntity.ok(authService.signup(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 400));
        }
    }

    // POST /api/auth/verify-otp
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody AuthDto.VerifyOtpRequest request,
                                       HttpServletResponse response) {
        try {
            AuthService.LoginResult result = authService.verifyOtp(request);
            setJwtCookie(response, result.getToken());
            return ResponseEntity.ok(result.getResponse());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 400));
        }
    }

    // POST /api/auth/resend-otp
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        try {
            return ResponseEntity.ok(authService.resendOtp(email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 400));
        }
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto.LoginRequest request,
                                   HttpServletResponse response) {
        try {
            AuthService.LoginResult result = authService.login(request);
            setJwtCookie(response, result.getToken());
            return ResponseEntity.ok(result.getResponse());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401)
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 401));
        }
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.forgotPassword(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 400));
        }
    }

    // POST /api/auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.resetPassword(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.ErrorResponse(e.getMessage(), 400));
        }
    }

    // POST /api/auth/logout — clear session + cookie
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Try to extract email from cookie to clear Redis session
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (jwtUtil.validateToken(token)) {
                            String email = jwtUtil.extractEmail(token);
                            authService.logout(email);
                        }
                        break;
                    }
                }
            }
            clearJwtCookie(response);
            return ResponseEntity.ok(new AuthDto.MessageResponse("Logged out successfully"));
        } catch (Exception e) {
            clearJwtCookie(response);
            return ResponseEntity.ok(new AuthDto.MessageResponse("Logged out"));
        }
    }

    // GET /api/auth/me — check who is logged in (reads JWT from cookie)
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        try {
            if (request.getCookies() == null) {
                return ResponseEntity.status(401)
                        .body(new AuthDto.ErrorResponse("Not authenticated", 401));
            }

            String token = null;
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }

            if (token == null || !jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new AuthDto.ErrorResponse("Not authenticated", 401));
            }

            String email = jwtUtil.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(new AuthDto.AuthResponse(
                    "Authenticated",
                    user.getUsername(), user.getEmail(), user.getRole()));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(new AuthDto.ErrorResponse("Not authenticated", 401));
        }
    }

    // GET /api/auth/health
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}