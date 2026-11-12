package com.example.backend.dto;

import com.example.backend.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeenAt;

    @JsonProperty("role")
    public Role getRole() {
        return roles;
    }

    public ParticipantDto(UUID userId, String displayName, String avatarUrl, Role roles) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
        this.online = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public ParticipantDto(UUID userId, String displayName, String avatarUrl, Role roles, Boolean online) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
        this.online = online;
        this.lastSeenAt = LocalDateTime.now();
    }
}
