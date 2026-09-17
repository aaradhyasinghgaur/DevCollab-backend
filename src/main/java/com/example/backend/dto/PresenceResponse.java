package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceResponse {
    private String type;
    private String status;
    private String roomId;
    private String userId;
    private String username;
    private String displayName;
    private String message;
    private List<ParticipantDto> participants;
    private Long timestamp;
}
