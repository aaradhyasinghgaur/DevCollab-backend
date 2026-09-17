package com.example.backend.respository;

import com.example.backend.entity.Room;
import com.example.backend.entity.RoomParticipant;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepo extends JpaRepository<RoomParticipant, UUID> {
    boolean existsByRoomAndUser(Room room , User user);
    List<RoomParticipant> findByRoom(Room room);
    List<RoomParticipant> findByUser(User user);
    Optional<RoomParticipant> findByRoomAndUser(Room room, User user);
    void deleteByRoomAndUser(Room room, User user);
}
