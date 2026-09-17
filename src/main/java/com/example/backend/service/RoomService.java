package com.example.backend.service;

import com.example.backend.dto.ParticipantDto;
import com.example.backend.dto.RoomResponse;
import com.example.backend.dto.RoomWithParticipants;
import com.example.backend.entity.*;
import com.example.backend.respository.ParticipantRepo;
import com.example.backend.respository.RoomRepo;
import com.example.backend.respository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RoomService {


    private final UserRepo userRepo;
    private final RoomRepo roomRepo;
    private final ParticipantRepo participantRepo;
    private final PresenceService presenceService;
    private final org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;

    public RoomService(UserRepo userRepo, RoomRepo roomRepo, ParticipantRepo participantRepo, PresenceService presenceService, org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate) {
        this.userRepo = userRepo;
        this.roomRepo = roomRepo;
        this.participantRepo = participantRepo;
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    public Room create(String roomName , String description , ProgrammingLanguage language , String username) {
        User owner = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Room room = Room.builder()
                .name(roomName)
                .description(description)
                .inviteCode(generateUniqueInviteCode())
                .language(language)
                .owner(owner)
                .build();

        Room savedRoom = roomRepo.save(room);

        RoomParticipant ownerParticipant = RoomParticipant.builder()
                .room(savedRoom)
                .user(owner)
                .role(Role.OWNER)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .build();

        participantRepo.save(ownerParticipant);

        return savedRoom;
    }

    public Room create(String roomName, ProgrammingLanguage language, String username) {
        return create(roomName, null, language, username);
    }

    private String generateUniqueInviteCode() {
        String code;
        do{
            code = randomCode();
        }while (roomRepo.existsByInviteCode(code));

        return code;
    }

    private String randomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();

        StringBuilder newCode = new StringBuilder();
        for(int i = 0; i < 8 ; ++i){
            newCode.append(chars.charAt(random.nextInt(chars.length())));
        }

        return newCode.toString();
    }


    public RoomWithParticipants join(String inviteCode, String participantUsername) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Invite code is required");
        }

        String cleanCode = inviteCode.trim().toUpperCase();

        Room room = roomRepo.findByInviteCodeIgnoreCase(cleanCode)
                .or(() -> roomRepo.findByInviteCode(cleanCode))
                .orElseThrow(() -> new RuntimeException("Room not found with invite code: " + inviteCode.trim()));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        User user = userRepo.findByUsername(participantUsername)
                .or(() -> userRepo.findByEmail(participantUsername))
                .orElseThrow(() -> new RuntimeException("User not found: " + participantUsername));

        boolean alreadyJoined = participantRepo.existsByRoomAndUser(room, user);

        if (!alreadyJoined) {
            RoomParticipant participant = RoomParticipant.builder()
                    .room(room)
                    .user(user)
                    .role(Role.EDITOR)
                    .joinedAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .build();

            participantRepo.save(participant);
        }

        List<RoomParticipant> allParticipants = participantRepo.findByRoom(room);

        return new RoomWithParticipants(room, allParticipants);
    }

    public List<RoomResponse> getUserRooms(String username) {
        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<RoomParticipant> memberships = participantRepo.findByUser(user);
        java.util.Map<java.util.UUID, Room> roomMap = new java.util.LinkedHashMap<>();

        for (RoomParticipant p : memberships) {
            if (p.getRoom().isActive()) {
                roomMap.put(p.getRoom().getId(), p.getRoom());
            }
        }

        List<Room> owned = roomRepo.findByOwnerOrderByCreatedAtDesc(user);
        for (Room r : owned) {
            if (r.isActive()) {
                roomMap.putIfAbsent(r.getId(), r);
            }
        }

        return roomMap.values().stream().map(this::mapToRoomResponse).toList();
    }

    public RoomResponse getRoomDetails(java.util.UUID roomId, String username) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        boolean isOwner = room.getOwner().getId().equals(user.getId());
        boolean isParticipant = participantRepo.existsByRoomAndUser(room, user);

        if (!isOwner && !isParticipant) {
            throw new RuntimeException("You are not a member of this room");
        }

        return mapToRoomResponse(room);
    }

    public RoomResponse getRoomByInviteCode(String inviteCode) {
        Room room = roomRepo.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Room not found with invite code: " + inviteCode));

        if (!room.isActive()) {
            throw new RuntimeException("Room is no longer active");
        }

        return mapToRoomResponse(room);
    }

    public RoomResponse updateRoom(java.util.UUID roomId, String name, ProgrammingLanguage language, String username) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!room.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the room owner can update room details");
        }

        if (name != null && !name.isBlank()) {
            room.setName(name);
        }
        if (language != null) {
            room.setLanguage(language);
        }

        Room saved = roomRepo.save(room);
        return mapToRoomResponse(saved);
    }

    public void deleteRoom(java.util.UUID roomId, String username) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!room.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the room owner can delete this room");
        }

        room.setActive(false);
        roomRepo.save(room);
        presenceService.removeRoom(roomId);

        // Broadcast to all active participants in the room that room has been deleted
        com.example.backend.dto.PresenceResponse deleteEvent = com.example.backend.dto.PresenceResponse.builder()
                .type("ROOM_DELETED")
                .status("ROOM_DELETED")
                .roomId(roomId.toString())
                .message("This room has been deleted by the owner.")
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", deleteEvent);
    }

    @org.springframework.transaction.annotation.Transactional
    public void leaveRoom(java.util.UUID roomId, String username) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Leaving room simply marks the user offline from the active room session;
        // membership is retained so the room stays in the user's dashboard.
        presenceService.removeUser(roomId, user.getId());

        participantRepo.findByRoomAndUser(room, user).ifPresent(p -> {
            p.setLastSeenAt(java.time.LocalDateTime.now());
            participantRepo.save(p);
        });

        // Broadcast OFFLINE status to room with updated participant presence
        RoomResponse updatedRoom = mapToRoomResponse(room);
        com.example.backend.dto.PresenceResponse leaveEvent = com.example.backend.dto.PresenceResponse.builder()
                .type("OFFLINE")
                .status("OFFLINE")
                .roomId(roomId.toString())
                .userId(user.getId().toString())
                .username(user.getUsername())
                .displayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
                .participants(updatedRoom.getParticipants())
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", leaveEvent);
    }

    @org.springframework.transaction.annotation.Transactional
    public void kickParticipant(java.util.UUID roomId, java.util.UUID targetUserId, String ownerUsername) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User owner = userRepo.findByUsername(ownerUsername)
                .or(() -> userRepo.findByEmail(ownerUsername))
                .orElseThrow(() -> new RuntimeException("Owner not found: " + ownerUsername));

        if (!room.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Only the room owner can kick participants from this room");
        }

        if (targetUserId.equals(owner.getId())) {
            throw new RuntimeException("Owner cannot kick themselves from the room");
        }

        User targetUser = userRepo.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found: " + targetUserId));

        // Permanently delete participant record
        participantRepo.deleteByRoomAndUser(room, targetUser);
        presenceService.removeUser(roomId, targetUserId);

        // Broadcast KICKED event to all room participants
        RoomResponse updatedRoom = mapToRoomResponse(room);
        com.example.backend.dto.PresenceResponse kickEvent = com.example.backend.dto.PresenceResponse.builder()
                .type("KICKED")
                .status("KICKED")
                .roomId(roomId.toString())
                .userId(targetUserId.toString())
                .username(targetUser.getUsername())
                .displayName(targetUser.getDisplayName() != null ? targetUser.getDisplayName() : targetUser.getUsername())
                .message("You have been removed from this room by the owner.")
                .participants(updatedRoom.getParticipants())
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/presence", kickEvent);
    }

    public RoomResponse mapToRoomResponse(Room room) {
        List<RoomParticipant> participants = participantRepo.findByRoom(room);
        java.util.Set<java.util.UUID> activeUserIds = presenceService.getActiveUserIds(room.getId());

        List<com.example.backend.dto.ParticipantDto> participantDtos = participants.stream()
                .map(p -> new com.example.backend.dto.ParticipantDto(
                        p.getUser().getId(),
                        p.getUser().getDisplayName() != null ? p.getUser().getDisplayName() : p.getUser().getUsername(),
                        p.getUser().getAvatarUrl(),
                        p.getRole(),
                        activeUserIds.contains(p.getUser().getId())
                ))
                .toList();

        return RoomResponse.builder()
                .roomId(room.getId())
                .inviteCode(room.getInviteCode())
                .ownerId(room.getOwner().getId())
                .roomName(room.getName())
                .description(room.getDescription())
                .language(room.getLanguage())
                .content(room.getContent())
                .revision(room.getRevision())
                .active(room.isActive())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .participants(participantDtos)
                .build();
    }
}
