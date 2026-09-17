package com.example.backend.respository;

import com.example.backend.entity.ExecutionResult;
import com.example.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionResultRepo extends JpaRepository<ExecutionResult, UUID> {
    List<ExecutionResult> findByRoomOrderByExecutionAtDesc(Room room);
}
