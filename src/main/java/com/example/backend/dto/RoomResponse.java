package com.example.backend.dto;

import com.example.backend.entity.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RoomResponse {
    private UUID roomId;
    private String inviteCode;
    private UUID ownerId;
    private String roomName;
    private String description;
    private ProgrammingLanguage language;
    private String content;
    private int revision;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ParticipantDto> participants;

    public UUID getId() {
        return roomId;
    }

    public String getName() {
        return roomName;
    }

    public RoomResponse(UUID id, String inviteCode, UUID ownerId, String name, ProgrammingLanguage language) {
        this.language = language;
        this.roomId = id;
        this.roomName = name;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
    }

    public RoomResponse(UUID id, String inviteCode, UUID ownerId, String name, ProgrammingLanguage language, List<ParticipantDto> participants) {
        this.language = language;
        this.roomId = id;
        this.roomName = name;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
        this.participants = participants;
    }
}
