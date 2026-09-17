package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "execution_results")
public class ExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id" , nullable = false )
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false )
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 30)
    private ProgrammingLanguage language;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    @Column(name = "exit_code" , nullable = false)
    private int exitCode;

    @Column(name = "duration_ms" , nullable = false)
    private int durationMs;

    @Column(name = "executed_at" , updatable = false, nullable = false )
    private LocalDateTime executionAt;

    @PrePersist
    public void onCreate(){
        executionAt= LocalDateTime.now();
    }

}
