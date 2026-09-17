package com.example.backend.auth;

import com.example.backend.config.JwtService;
import com.example.backend.entity.AuthProvider;
import com.example.backend.entity.User;
import com.example.backend.respository.RegisterRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RegisterRepo repo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register (RegisterRequest request){
        if (repo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }
        if (repo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getUsername())
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = repo.save(user);

        return getAuthenticationResponse(savedUser);
    }

    public AuthenticationResponse authenticate (AuthenticationRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repo.findByEmail(request.getEmail())
                .or(() -> repo.findByUsername(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        return getAuthenticationResponse(user);
    }

    public AuthenticationResponse getAuthenticationResponse(User user){
        var jwtToken = jwtService.generateToken((UserDetails) user);

        //generate and save refresh token

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

}
