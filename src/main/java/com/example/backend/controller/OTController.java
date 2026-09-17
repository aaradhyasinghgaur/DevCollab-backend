package com.example.backend.controller;

import com.example.backend.dto.OTBroadcastMessage;
import com.example.backend.dto.OTSubmitMessage;
import com.example.backend.service.OTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OTController {

    private final OTService otService;

    // REST: Get latest room code content and revision
    @GetMapping("/api/rooms/{roomId}/code")
    public ResponseEntity<OTService.RoomCodeContent> getRoomCode(@PathVariable UUID roomId) {
        return ResponseEntity.ok(otService.getRoomContent(roomId));
    }

    // STOMP: Process incoming client code transformation
    @MessageMapping("/room/{roomId}/code.edit")
    public void handleCodeEdit(
            @DestinationVariable UUID roomId,
            @Payload OTSubmitMessage submitMessage,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "anonymous";
        otService.processSubmitMessage(roomId, submitMessage, username);
    }
}
