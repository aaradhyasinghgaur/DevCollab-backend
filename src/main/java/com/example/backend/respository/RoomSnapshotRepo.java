package com.example.backend.respository;

import com.example.backend.entity.Room;
import com.example.backend.entity.RoomSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomSnapshotRepo extends JpaRepository<RoomSnapshot, UUID> {
    Optional<RoomSnapshot> findTopByRoomOrderByRevisionDesc(Room room);
}
