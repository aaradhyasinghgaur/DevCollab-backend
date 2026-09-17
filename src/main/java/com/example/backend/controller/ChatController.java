package com.example.backend.controller;

import com.example.backend.dto.ChatMessageDto;
import com.example.backend.dto.CursorPayload;
import com.example.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final com.example.backend.respository.UserRepo userRepo;
    private final com.example.backend.service.PresenceService presenceService;
    private final com.example.backend.service.RoomService roomService;
    private final com.example.backend.respository.RoomRepo roomRepo;

    // REST: Get room chat history
    @GetMapping("/api/rooms/{roomId}/chat")
    public List<ChatMessageDto> getChatHistory(@PathVariable UUID roomId) {
        return chatService.getRoomChatHistory(roomId);
    }

    // STOMP: Send message to specific room
    @MessageMapping("/room/{roomId}/chat.send")
    public ChatMessageDto sendRoomMessage(
            @DestinationVariable UUID roomId,
            @Payload ChatMessageDto message,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String sender = principal != null ? principal.getName() : message.getSenderName();
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("roomId", roomId.toString());
            headerAccessor.getSessionAttributes().put("username", sender);
        }
        return chatService.processAndBroadcast(roomId, message.getContent(), sender, MessageType.CHAT);
    }

    // STOMP: Room presence (join/leave/heartbeat)
    @MessageMapping("/room/{roomId}/presence")
    public void updatePresence(
            @DestinationVariable UUID roomId,
            @Payload java.util.Map<String, Object> payload,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = principal != null ? principal.getName() : (String) payload.get("username");
        String status = (String) payload.getOrDefault("status", "ACTIVE");
        String userIdStr = (String) payload.get("userId");

        com.example.backend.entity.User user = null;
        if (username != null) {
            user = userRepo.findByUsername(username).or(() -> userRepo.findByEmail(username)).orElse(null);
        }
        if (user == null && userIdStr != null) {
            try {
                user = userRepo.findById(UUID.fromString(userIdStr)).orElse(null);
            } catch (Exception ignored) {}
        }

        if (user != null) {
            UUID userId = user.getId();
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("roomId", roomId.toString());
                headerAccessor.getSessionAttributes().put("username", user.getUsername());
                headerAccessor.getSessionAttributes().put("userId", userId.toString());
            }

            if ("LEAVE".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
                presenceService.removeUser(roomId, userId);
            } else {
                presenceService.recordHeartbeat(roomId, new com.example.backend.dto.ParticipantDto(
                        userId,
                        user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                        user.getAvatarUrl(),
                        com.example.backend.entity.Role.EDITOR,
                        true
                ));
            }

            com.example.backend.entity.Room room = roomRepo.findById(roomId).orElse(null);
            List<com.example.backend.dto.ParticipantDto> participants = room != null
                    ? roomService.mapToRoomResponse(room).getParticipants()
                    : java.util.Collections.emptyList();

            com.example.backend.dto.PresenceResponse presenceEvent = com.example.backend.dto.PresenceResponse.builder()
                    .type(status)
                    .status(status)
                    .roomId(roomId.toString())
                    .userId(userId.toString())
                    .username(user.getUsername())
                    .displayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
                    .participants(participants)
                    .timestamp(System.currentTimeMillis())
                    .build();
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", presenceEvent);
        }
    }

    // STOMP: Real-time cursor coordinates
    @MessageMapping("/room/{roomId}/cursor")
    public void sendCursor(
            @DestinationVariable UUID roomId,
            @Payload CursorPayload cursor,
            Principal principal
    ) {
        if (principal != null) {
            String username = principal.getName();
            userRepo.findByUsername(username)
                    .or(() -> userRepo.findByEmail(username))
                    .ifPresent(u -> {
                        cursor.setUserId(u.getId());
                        cursor.setUsername(u.getUsername());
                        cursor.setDisplayName(u.getDisplayName());
                        String[] colors = {"#a855f7", "#3b82f6", "#10b981", "#f59e0b", "#ec4899", "#06b6d4", "#f97316"};
                        int colorIdx = Math.abs(u.getId().hashCode()) % colors.length;
                        cursor.setColor(colors[colorIdx]);
                    });
        }
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/cursor", cursor);
    }

    // Backward-compatible fallback for simple public chat demo
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        }
        return chatMessage;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }
}

