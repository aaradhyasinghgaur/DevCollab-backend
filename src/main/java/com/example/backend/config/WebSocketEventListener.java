package com.example.backend.config;

import com.example.backend.dto.RoomResponse;
import com.example.backend.entity.Room;
import com.example.backend.respository.RoomRepo;
import com.example.backend.service.PresenceService;
import com.example.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messageTemplate;
    private final PresenceService presenceService;
    private final RoomService roomService;
    private final RoomRepo roomRepo;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getSessionAttributes() != null) {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            String roomIdStr = (String) headerAccessor.getSessionAttributes().get("roomId");
            String userIdStr = (String) headerAccessor.getSessionAttributes().get("userId");

            if (roomIdStr != null && userIdStr != null) {
                try {
                    UUID roomId = UUID.fromString(roomIdStr);
                    UUID userId = UUID.fromString(userIdStr);
                    log.info("User Disconnected: {} (ID: {}) from room: {}", username, userId, roomId);

                    presenceService.removeUser(roomId, userId);

                    Room room = roomRepo.findById(roomId).orElse(null);
                    if (room != null && room.isActive()) {
                        RoomResponse roomResponse = roomService.mapToRoomResponse(room);
                        com.example.backend.dto.PresenceResponse offlineEvent = com.example.backend.dto.PresenceResponse.builder()
                                .type("OFFLINE")
                                .status("OFFLINE")
                                .roomId(roomIdStr)
                                .userId(userIdStr)
                                .username(username != null ? username : "")
                                .participants(roomResponse.getParticipants())
                                .timestamp(System.currentTimeMillis())
                                .build();
                        messageTemplate.convertAndSend("/topic/room/" + roomId + "/presence", offlineEvent);
                    }
                } catch (Exception e) {
                    log.warn("Error handling disconnect: {}", e.getMessage());
                }
            }
        }
    }
}
