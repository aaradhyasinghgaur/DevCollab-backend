package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_participants" ,
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id" , nullable = false )
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "joined_at" , nullable = false , updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_seen_at" , nullable = false)
    private LocalDateTime lastSeenAt;

    //functions
    @PrePersist
    public void onCreate(){
        joinedAt = LocalDateTime.now();
        lastSeenAt = LocalDateTime.now();
    }
}
