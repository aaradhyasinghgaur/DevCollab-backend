package com.example.backend.dto;

import com.example.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@lombok.Builder
public class ParticipantDto {
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private Role roles;
    private Boolean online;

    public ParticipantDto(UUID userId, String displayName, String avatarUrl, Role roles) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
        this.online = true;
    }
}
