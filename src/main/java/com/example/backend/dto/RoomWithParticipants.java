package com.example.backend.dto; // or a package like .service.model, your call

import com.example.backend.entity.Room;
import com.example.backend.entity.RoomParticipant;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// to move info from service layer to the controller layer.
@Getter
@Setter
public class RoomWithParticipants {
    private final Room room;
    private final List<RoomParticipant> participants;

    public RoomWithParticipants(Room room, List<RoomParticipant> participants) {
        this.room = room;
        this.participants = participants;
    }
}