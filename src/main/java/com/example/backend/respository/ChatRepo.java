package com.example.backend.respository;

import com.example.backend.entity.ChatMessage;
import com.example.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepo extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByRoomOrderByCreatedAtAsc(Room room);
}
