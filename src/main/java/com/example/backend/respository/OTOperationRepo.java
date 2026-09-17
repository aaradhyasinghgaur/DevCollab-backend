package com.example.backend.respository;

import com.example.backend.entity.OTOperation;
import com.example.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OTOperationRepo extends JpaRepository<OTOperation, UUID> {
    List<OTOperation> findByRoomAndRevisionGreaterThanOrderByRevisionAsc(Room room, int revision);
}
