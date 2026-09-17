package com.example.backend.controller;


// provide :- name , language , extract owner from the jwt token you already have in your database.
// return :- roomId , inviteCode , ownerId ,  room name , language.

import com.example.backend.dto.*;
import com.example.backend.entity.Room;
import com.example.backend.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RoomController {

    public final RoomService service;

    @Autowired
    public RoomController(RoomService service){
        this.service = service;
    }

    @PostMapping("/createRoom")
    public RoomResponse createRoom(@RequestBody CreateRoomRequest request , Authentication authentication){
        String requestUsername = authentication.getName();
        Room room = service.create(request.getName(), request.getDescription(), request.getLanguage(), requestUsername);
        return service.mapToRoomResponse(room);
    }

    @PostMapping("/rooms")
    public RoomResponse createRoomRest(@RequestBody CreateRoomRequest request, Authentication authentication) {
        return createRoom(request, authentication);
    }

    @PostMapping("/joinRoom")
    public RoomResponse joinRoom (@RequestBody JoinRoomRequest request, Authentication authentication){
        String participantUsername = authentication.getName();
        RoomWithParticipants room = service.join(request.getInviteCode() , participantUsername);
        return service.mapToRoomResponse(room.getRoom());
    }

    @PostMapping("/rooms/join")
    public RoomResponse joinRoomRest(@RequestBody JoinRoomRequest request, Authentication authentication) {
        return joinRoom(request, authentication);
    }

    @GetMapping("/rooms")
    public List<RoomResponse> getUserRooms(Authentication authentication) {
        return service.getUserRooms(authentication.getName());
    }

    @GetMapping("/rooms/{id}")
    public RoomResponse getRoomById(@PathVariable java.util.UUID id, Authentication authentication) {
        return service.getRoomDetails(id, authentication.getName());
    }

    @GetMapping("/rooms/code/{inviteCode}")
    public RoomResponse getRoomByCode(@PathVariable String inviteCode) {
        return service.getRoomByInviteCode(inviteCode);
    }

    @PutMapping("/rooms/{id}")
    public RoomResponse updateRoom(@PathVariable java.util.UUID id,
                                   @RequestBody UpdateRoomRequest request,
                                   Authentication authentication) {
        return service.updateRoom(id, request.getName(), request.getLanguage(), authentication.getName());
    }

    @DeleteMapping("/rooms/{id}")
    public org.springframework.http.ResponseEntity<Void> deleteRoom(@PathVariable java.util.UUID id, Authentication authentication) {
        service.deleteRoom(id, authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{id}/leave")
    public org.springframework.http.ResponseEntity<Void> leaveRoom(@PathVariable java.util.UUID id, Authentication authentication) {
        service.leaveRoom(id, authentication.getName());
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{id}/participants/{userId}")
    public org.springframework.http.ResponseEntity<Void> kickParticipant(
            @PathVariable java.util.UUID id,
            @PathVariable java.util.UUID userId,
            Authentication authentication
    ) {
        service.kickParticipant(id, userId, authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{id}/kick/{userId}")
    public org.springframework.http.ResponseEntity<Void> kickParticipantPost(
            @PathVariable java.util.UUID id,
            @PathVariable java.util.UUID userId,
            Authentication authentication
    ) {
        service.kickParticipant(id, userId, authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}

