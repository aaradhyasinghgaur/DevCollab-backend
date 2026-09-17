package com.example.backend.config;

import com.example.backend.entity.AuthProvider;
import com.example.backend.entity.User;
import com.example.backend.respository.RegisterRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RegisterRepo repo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        System.out.println("LOAD USER CALLED");

        //getting user info from google/github.
        OAuth2User oauth2User = super.loadUser(userRequest);

        System.out.println(oauth2User.getAttributes());

        String provider = userRequest.getClientRegistration().getRegistrationId();
        processOAuthUser(oauth2User , AuthProvider.valueOf(provider.toUpperCase()));

        return oauth2User;
    }

    public User processOAuthUser(OAuth2User oauth2User, AuthProvider provider) {
        String email;
        String username;
        String avatarUrl;
        String providerId;

        String displayName;

        if(provider == AuthProvider.GOOGLE){
            email = oauth2User.getAttribute("email");
            displayName = oauth2User.getAttribute("name");
            avatarUrl = oauth2User.getAttribute("picture");
            providerId = oauth2User.getAttribute("sub");
            username = (email != null && email.contains("@")) ? email.split("@")[0] : displayName;
        }
        else if (provider == AuthProvider.GITHUB){
            email = oauth2User.getAttribute("email");
            // GitHub username is in "login", while "name" is display name (often null)
            username = oauth2User.getAttribute("login");
            displayName = oauth2User.getAttribute("name");
            avatarUrl = oauth2User.getAttribute("avatar_url");

            Object id = oauth2User.getAttribute("id");
            providerId = id != null ? id.toString() : null;

            if (username == null || username.isBlank()) {
                username = displayName;
            }
        }
        else{
            throw new IllegalArgumentException("Invalid Auth provider: " + provider);
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = username != null ? username : "User";
        }

        // Workaround when GitHub email is private
        if (email == null || email.isBlank()) {
            email = providerId + "@github.local";
        }

        // Check if user already exists by email
        Optional<User> existingUser = repo.findByEmail(email);
        if(existingUser.isPresent()){
            return existingUser.get();
        }

        if (username == null || username.isBlank()) {
            username = provider.name().toLowerCase() + "_user_" + (providerId != null ? providerId : System.currentTimeMillis());
        }

        // Sanitize username and ensure uniqueness
        String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitizedUsername.length() > 40) {
            sanitizedUsername = sanitizedUsername.substring(0, 40);
        }
        String candidateUsername = sanitizedUsername;
        int count = 1;
        while (repo.existsByUsername(candidateUsername)) {
            candidateUsername = sanitizedUsername + "_" + count++;
        }

        User user = User.builder()
                .email(email)
                .username(candidateUsername)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .provider(provider)
                .providerId(providerId)
                .build();

        return repo.save(user);
    }
}
