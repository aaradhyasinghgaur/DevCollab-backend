package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//should user entity implement userDetails.
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username" , nullable = false , unique = true, length = 50)
    private String username;

    @Column(nullable = false , unique = true, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name" , nullable = false , length = 100)
    private String displayName;

    @Column(name = "avatar_url" , length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 20)
    private AuthProvider provider;

    @Column( name = "provider_id" , length = 255)
    private String providerId;

    @Column(name = "is_active" , nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at" , nullable = false )
    private LocalDateTime updatedAt;

    //relationship both ways .
    @OneToMany(mappedBy = "user")
    private java.util.List<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "owner")
    private java.util.List<Room> ownedRooms;

    @OneToMany(mappedBy = "user")
    private java.util.List<RoomParticipant> roomParticipants;

    @OneToMany(mappedBy = "user")
    private java.util.List<OTOperation> operations;

    @OneToMany(mappedBy = "user")
    private java.util.List<ChatMessage> chatMessages;

    @OneToMany(mappedBy = "user")
    private java.util.List<ExecutionResult> executionResults;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
        // or List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();

    }

}
