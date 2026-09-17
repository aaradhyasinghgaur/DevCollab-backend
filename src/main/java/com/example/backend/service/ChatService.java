package com.example.backend.service;

import com.example.backend.controller.MessageType;
import com.example.backend.dto.ChatMessageDto;
import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.Room;
import com.example.backend.entity.User;
import com.example.backend.respository.ChatRepo;
import com.example.backend.respository.RoomRepo;
import com.example.backend.respository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepo chatRepo;
    private final RoomRepo roomRepo;
    private final UserRepo userRepo;
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public ChatMessageDto processAndBroadcast(UUID roomId, String content, String username, MessageType type) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .user(user)
                .content(content != null ? content.trim() : "")
                .build();

        // Only persist actual chat messages with non-empty content
        if (type == MessageType.CHAT && !message.getContent().isEmpty()) {
            message = chatRepo.save(message);
        }

        ChatMessageDto dto = ChatMessageDto.builder()
                .id(message.getId() != null ? message.getId() : UUID.randomUUID())
                .roomId(roomId)
                .senderId(user.getId())
                .senderName(user.getDisplayName())
                .senderAvatar(user.getAvatarUrl())
                .content(content)
                .type(type)
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRoomChatHistory(UUID roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        List<ChatMessage> history = chatRepo.findByRoomOrderByCreatedAtAsc(room);
        return history.stream()
                .map(m -> ChatMessageDto.builder()
                        .id(m.getId())
                        .roomId(roomId)
                        .senderId(m.getUser().getId())
                        .senderName(m.getUser().getDisplayName())
                        .senderAvatar(m.getUser().getAvatarUrl())
                        .content(m.getContent())
                        .type(MessageType.CHAT)
                        .createdAt(m.getCreatedAt())
                        .build()
                )
                .toList();
    }
}
