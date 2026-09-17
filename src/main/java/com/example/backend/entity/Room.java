package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table (name = "rooms")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column( length = 100 , nullable = false)
    private String name ;

    @Column(name = "invite_code" , unique = true, length = 8 , nullable = false)
    private String inviteCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30 , nullable = false)
    private ProgrammingLanguage language;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String content = "";

    @Column(nullable = false )
    @Builder.Default
    private int revision = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id" , nullable = false) //should be one of the existing users.
    private User owner;

    @Column(name = "is_active" , nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at" , nullable = false)
    private LocalDateTime updatedAt;

    //relationships
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<RoomParticipant> participants;

    @OneToMany(mappedBy = "room")
    public List<OTOperation> otOperations;

    @OneToMany(mappedBy = "room")
    public List<RoomSnapshot> snapshots;

    @OneToMany(mappedBy = "room")
    public List<ChatMessage> chatMessages;

    @OneToMany(mappedBy = "room")
    public List<ExecutionResult> executionResults;

    //

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
