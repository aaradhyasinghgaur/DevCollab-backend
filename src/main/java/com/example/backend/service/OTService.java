package com.example.backend.service;

import com.example.backend.dto.OTBroadcastMessage;
import com.example.backend.dto.OTSubmitMessage;
import com.example.backend.entity.OTOperation;
import com.example.backend.entity.Room;
import com.example.backend.entity.RoomSnapshot;
import com.example.backend.entity.User;
import com.example.backend.ot.OTEngine;
import com.example.backend.ot.TextOperation;
import com.example.backend.respository.OTOperationRepo;
import com.example.backend.respository.RoomRepo;
import com.example.backend.respository.RoomSnapshotRepo;
import com.example.backend.respository.UserRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTService {

    private final RoomRepo roomRepo;
    private final UserRepo userRepo;
    private final OTOperationRepo otOperationRepo;
    private final RoomSnapshotRepo roomSnapshotRepo;
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    private static final int SNAPSHOT_INTERVAL = 25;

    @Transactional
    public OTBroadcastMessage processSubmitMessage(UUID roomId, OTSubmitMessage submitMessage, String username) {
        // Synchronize on room ID string lock to serialize concurrent revisions
        synchronized (roomId.toString().intern()) {
            Room room = roomRepo.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

            User user = null;
            if (username != null && !"anonymous".equalsIgnoreCase(username)) {
                user = userRepo.findByUsername(username)
                        .or(() -> userRepo.findByEmail(username))
                        .orElse(null);
            }
            if (user == null && submitMessage.getUserId() != null) {
                user = userRepo.findById(submitMessage.getUserId()).orElse(null);
            }
            if (user == null) {
                user = room.getOwner();
            }

            String currentContent = room.getContent() != null ? room.getContent() : "";
            String updatedContent = currentContent;
            TextOperation clientOp = submitMessage.getOperation();

            if (clientOp != null && clientOp.getComponents() != null && !clientOp.getComponents().isEmpty()) {
                // Fetch concurrent operations that occurred since client's base revision
                List<OTOperation> concurrentOps = otOperationRepo.findByRoomAndRevisionGreaterThanOrderByRevisionAsc(room, submitMessage.getRevision());

                TextOperation transformed = clientOp;
                for (OTOperation opRecord : concurrentOps) {
                    try {
                        TextOperation serverOp = objectMapper.readValue(opRecord.getOperationData(), TextOperation.class);
                        transformed = OTEngine.transform(transformed, serverOp, OTEngine.Priority.LEFT);
                    } catch (Exception e) {
                        log.error("Failed to parse historical operation for room {}: {}", roomId, e.getMessage());
                    }
                }

                updatedContent = OTEngine.apply(currentContent, transformed);
                clientOp = transformed;
            } else {
                // Direct Delta or Full Content
                if (submitMessage.getType() != null) {
                    updatedContent = applyDirectDelta(currentContent, submitMessage.getType(), submitMessage.getPosition(), submitMessage.getText(), submitMessage.getLength(), submitMessage.getFullContent());
                    clientOp = createTextOpFromDelta(submitMessage.getType(), submitMessage.getPosition(), submitMessage.getText(), submitMessage.getLength());
                } else if (submitMessage.getFullContent() != null) {
                    updatedContent = submitMessage.getFullContent();
                    clientOp = new TextOperation().insert(updatedContent);
                }
            }

            // Apply transformed / delta content to current room document
            room.setContent(updatedContent);

            int newRevision = room.getRevision() + 1;
            room.setRevision(newRevision);

            // Persist the transformation operation
            String serializedOp = "";
            try {
                serializedOp = objectMapper.writeValueAsString(clientOp != null ? clientOp : submitMessage);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize OT operation", e);
            }

            OTOperation operationEntity = OTOperation.builder()
                    .room(room)
                    .user(user)
                    .revision(newRevision)
                    .operationData(serializedOp)
                    .build();
            otOperationRepo.save(operationEntity);

            // Periodic snapshot save
            if (newRevision % SNAPSHOT_INTERVAL == 0) {
                RoomSnapshot snapshot = RoomSnapshot.builder()
                        .room(room)
                        .content(updatedContent)
                        .revision(newRevision)
                        .build();
                roomSnapshotRepo.save(snapshot);
                log.info("Saved snapshot for room {} at revision {}", roomId, newRevision);
            }

            roomRepo.save(room);

            OTBroadcastMessage broadcast = OTBroadcastMessage.builder()
                    .roomId(roomId)
                    .userId(user.getId())
                    .clientId(submitMessage.getClientId())
                    .username(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
                    .revision(newRevision)
                    .type(submitMessage.getType())
                    .position(submitMessage.getPosition())
                    .text(submitMessage.getText())
                    .length(submitMessage.getLength())
                    .range(submitMessage.getRange())
                    .fullContent(updatedContent)
                    .operation(clientOp)
                    .build();

            // Broadcast to room channel
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/code", broadcast);

            return broadcast;
        }
    }

    @Transactional
    public OTBroadcastMessage processOperation(UUID roomId, int clientRevision, TextOperation clientOp, String username) {
        OTSubmitMessage submit = OTSubmitMessage.builder()
                .revision(clientRevision)
                .operation(clientOp)
                .build();
        return processSubmitMessage(roomId, submit, username);
    }

    private String applyDirectDelta(String current, String type, Integer position, String text, Integer length, String fullContent) {
        if (current == null) current = "";
        int pos = position != null ? Math.max(0, Math.min(position, current.length())) : current.length();

        if ("INSERT".equalsIgnoreCase(type)) {
            String insertText = text != null ? text : "";
            return current.substring(0, pos) + insertText + current.substring(pos);
        } else if ("DELETE".equalsIgnoreCase(type)) {
            int len = length != null ? Math.max(0, length) : 0;
            int end = Math.min(pos + len, current.length());
            return current.substring(0, pos) + current.substring(end);
        } else if ("REPLACE".equalsIgnoreCase(type)) {
            int len = length != null ? Math.max(0, length) : 0;
            int end = Math.min(pos + len, current.length());
            String insertText = text != null ? text : "";
            return current.substring(0, pos) + insertText + current.substring(end);
        } else if (fullContent != null) {
            return fullContent;
        }
        return current;
    }

    private TextOperation createTextOpFromDelta(String type, Integer position, String text, Integer length) {
        TextOperation op = new TextOperation();
        int pos = position != null ? Math.max(0, position) : 0;
        if (pos > 0) {
            op.retain(pos);
        }
        if ("INSERT".equalsIgnoreCase(type)) {
            op.insert(text != null ? text : "");
        } else if ("DELETE".equalsIgnoreCase(type)) {
            int len = length != null ? Math.max(0, length) : 0;
            if (len > 0) {
                op.delete(len);
            }
        } else if ("REPLACE".equalsIgnoreCase(type)) {
            int len = length != null ? Math.max(0, length) : 0;
            if (len > 0) {
                op.delete(len);
            }
            op.insert(text != null ? text : "");
        }
        return op;
    }

    public RoomCodeContent getRoomContent(UUID roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
        return new RoomCodeContent(room.getId(), room.getContent(), room.getRevision(), room.getLanguage().name());
    }

    public record RoomCodeContent(UUID roomId, String content, int revision, String language) {}
}
