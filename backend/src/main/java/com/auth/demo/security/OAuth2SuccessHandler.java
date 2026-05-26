package com.auth.demo.security;

import com.auth.demo.model.User;
import com.auth.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    public OAuth2SuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=Email+not+found+from+OAuth2+provider");
            return;
        }

        // Check if user exists
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            // Register new user
            user = new User();
            user.setEmail(email);
            // Default username to email prefix if not provided
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter;
                counter++;
            }
            user.setUsername(username);
            user.setName(name);
            user.setPassword(java.util.UUID.randomUUID().toString());
            user.setProvider("google");
            user.setIsVerified(true); // Google already verified their email
            user.setRole("USER");
            user = userRepository.save(user);
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole(), user.getUsername());

        // Set HttpOnly Cookie
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());

        // Redirect to frontend (frontend will just route to /problems)
        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
