package com.example.backend.dto;

import com.example.backend.ot.TextOperation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTBroadcastMessage {
    private UUID roomId;
    private UUID userId;
    private String clientId;

    @JsonProperty("senderId")
    public UUID getSenderId() {
        return userId;
    }

    private String username;

    @JsonProperty("senderName")
    public String getSenderName() {
        return username;
    }

    private int revision;

    // Direct atomic delta fields
    private String type; // 'INSERT', 'DELETE', 'REPLACE'
    private Integer position;
    private String text;
    private Integer length;
    private OTSubmitMessage.RangeDto range;
    private String fullContent;

    // Structured OT components (optional)
    private TextOperation operation;
}
