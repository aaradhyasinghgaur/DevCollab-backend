package com.example.backend.dto;

import com.example.backend.controller.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;
}
