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

        // Always keep local in-memory fallback updated
        localPresenceMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(participant.getUserId(), participant);
        localPresenceExpiry.put(participant.getUserId(), System.currentTimeMillis() + 60_000);

        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.opsForHash().put(key, participant.getUserId().toString(), participant);
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Redis presence write fallback to local: {}", e.getMessage());
        }
    }

    public void removeUser(UUID roomId, UUID userId) {
        if (userId == null) return;
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
        }
        localPresenceExpiry.remove(userId);

        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.opsForHash().delete(key, userId.toString());
        } catch (Exception e) {
            log.debug("Redis presence remove fallback to local: {}", e.getMessage());
        }
    }

    public void removeRoom(UUID roomId) {
        if (roomId == null) return;
        localPresenceMap.remove(roomId);
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis presence removeRoom fallback: {}", e.getMessage());
        }
    }

    public List<ParticipantDto> getActiveParticipants(UUID roomId) {
        Map<UUID, ParticipantDto> result = new LinkedHashMap<>();

        // 1. Check local cache (prune expired)
        long now = System.currentTimeMillis();
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers != null) {
            for (Map.Entry<UUID, ParticipantDto> entry : roomUsers.entrySet()) {
                Long expiry = localPresenceExpiry.get(entry.getKey());
                if (expiry != null && expiry > now) {
                    ParticipantDto p = entry.getValue();
                    p.setOnline(true);
                    result.put(p.getUserId(), p);
                } else {
                    roomUsers.remove(entry.getKey());
                    localPresenceExpiry.remove(entry.getKey());
                }
            }
        }

        // 2. Check Redis
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries != null && !entries.isEmpty()) {
                for (Object value : entries.values()) {
                    if (value instanceof ParticipantDto p) {
                        p.setOnline(true);
                        result.put(p.getUserId(), p);
                    } else if (value instanceof Map<?, ?> m) {
                        try {
                            Object idVal = m.get("userId");
                            UUID uId = idVal != null ? UUID.fromString(idVal.toString()) : null;
                            String dName = m.get("displayName") != null ? m.get("displayName").toString() : null;
                            String aUrl = m.get("avatarUrl") != null ? m.get("avatarUrl").toString() : null;
                            if (uId != null) {
                                ParticipantDto p = new ParticipantDto(uId, dName, aUrl, Role.EDITOR, true);
                                result.put(uId, p);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Redis presence read fallback: {}", e.getMessage());
        }

        return new ArrayList<>(result.values());
    }

    public Set<UUID> getActiveUserIds(UUID roomId) {
        Set<UUID> activeIds = new HashSet<>();

        // 1. Direct hash key query on Redis (keys are user UUID strings)
        try {
            String key = PRESENCE_KEY_PREFIX + roomId;
            Set<Object> keys = redisTemplate.opsForHash().keys(key);
            if (keys != null && !keys.isEmpty()) {
                for (Object k : keys) {
                    try {
                        activeIds.add(UUID.fromString(k.toString()));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.debug("Redis keys check fallback: {}", e.getMessage());
        }

        // 2. Merge with active participants list (includes local in-memory fallback)
        for (ParticipantDto p : getActiveParticipants(roomId)) {
            if (p != null && p.getUserId() != null) {
                activeIds.add(p.getUserId());
            }
        }
        return activeIds;
    }
}
