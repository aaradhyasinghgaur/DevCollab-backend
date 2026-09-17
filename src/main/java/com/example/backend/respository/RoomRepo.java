package com.example.backend.respository; // matching your existing package spelling

import com.example.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepo extends JpaRepository<Room, UUID> {
    boolean existsByInviteCode(String inviteCode);
    Optional<Room> findByInviteCode(String roomCode);
    Optional<Room> findByInviteCodeIgnoreCase(String inviteCode);
    List<Room> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<Room> findByIdAndActiveTrue(UUID id);
}
