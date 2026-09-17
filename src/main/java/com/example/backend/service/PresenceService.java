package com.example.backend.service;

import com.example.backend.dto.ParticipantDto;
import com.example.backend.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback map: roomId -> (userId -> ParticipantDto)
    private final Map<UUID, Map<UUID, ParticipantDto>> localPresenceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> localPresenceExpiry = new ConcurrentHashMap<>();

    private static final String PRESENCE_KEY_PREFIX = "room:presence:";

    public void recordHeartbeat(UUID roomId, ParticipantDto participant) {
        if (participant == null || participant.getUserId() == null) {
            return;
        }
        participant.setOnline(true);

        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.opsForHash().put(key, participant.getUserId().toString(), participant);
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Redis presence write fallback to local: {}", e.getMessage());
            localPresenceMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                    .put(participant.getUserId(), participant);
            localPresenceExpiry.put(participant.getUserId(), System.currentTimeMillis() + 60_000);
        }
    }

    public void removeUser(UUID roomId, UUID userId) {
        if (userId == null) return;
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.opsForHash().delete(key, userId.toString());
        } catch (Exception e) {
            log.debug("Redis presence remove fallback to local: {}", e.getMessage());
        }
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
        }
        localPresenceExpiry.remove(userId);
    }

    public void removeRoom(UUID roomId) {
        if (roomId == null) return;
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis presence removeRoom fallback: {}", e.getMessage());
        }
        localPresenceMap.remove(roomId);
    }

    public List<ParticipantDto> getActiveParticipants(UUID roomId) {
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                List<ParticipantDto> list = new ArrayList<>();
                for (Object value : entries.values()) {
                    if (value instanceof ParticipantDto p) {
                        p.setOnline(true);
                        list.add(p);
                    }
                }
                return list;
            }
        } catch (Exception e) {
            log.debug("Redis presence read fallback to local: {}", e.getMessage());
        }

        // Local fallback check
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers == null) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<ParticipantDto> active = new ArrayList<>();
        for (Map.Entry<UUID, ParticipantDto> entry : roomUsers.entrySet()) {
            Long expiry = localPresenceExpiry.get(entry.getKey());
            if (expiry != null && expiry > now) {
                ParticipantDto p = entry.getValue();
                p.setOnline(true);
                active.add(p);
            }
        }
        return active;
    }

    public Set<UUID> getActiveUserIds(UUID roomId) {
        Set<UUID> activeIds = new HashSet<>();
        for (ParticipantDto p : getActiveParticipants(roomId)) {
            if (p != null && p.getUserId() != null) {
                activeIds.add(p.getUserId());
            }
        }
        return activeIds;
    }
}
