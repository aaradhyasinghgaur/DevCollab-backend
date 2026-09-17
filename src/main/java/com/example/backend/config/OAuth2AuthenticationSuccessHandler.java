package com.example.backend.config;

import com.example.backend.auth.AuthenticationResponse;
import com.example.backend.entity.AuthProvider;
import com.example.backend.entity.User;
import com.example.backend.respository.RegisterRepo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RegisterRepo repo;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtService jwtService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication)
            throws IOException, ServletException {

        System.out.println("SUCCESS HANDLER CALLED");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email"); // may be null

        System.out.println("from OAuth2AuthenticationSuccessHandler: " + email);

        // to fetch provider
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AuthProvider provider = AuthProvider.valueOf(token.getAuthorizedClientRegistrationId().toUpperCase());

        User user;
        if (email == null || !repo.existsByEmail(email)) {
            user = customOAuth2UserService.processOAuthUser(oAuth2User, provider);
        } else {
            user = repo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        }

        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        if (user.getEmail() != null) extraClaims.put("email", user.getEmail());
        if (user.getDisplayName() != null) extraClaims.put("displayName", user.getDisplayName());
        if (user.getId() != null) extraClaims.put("userId", user.getId().toString());
        if (user.getAvatarUrl() != null) extraClaims.put("avatarUrl", user.getAvatarUrl());
        String jwt = jwtService.generateToken(extraClaims, user);

        AuthenticationResponse authenticationResponse =
                AuthenticationResponse.builder()
                        .token(jwt)
                        .build();

        String baseFrontendUrl = (frontendUrl != null && frontendUrl.endsWith("/"))
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : (frontendUrl != null ? frontendUrl : "http://localhost:3000");

        response.setStatus(HttpServletResponse.SC_OK);
        response.sendRedirect(baseFrontendUrl + "/oauth-success?token=" + jwt);
    }
}
