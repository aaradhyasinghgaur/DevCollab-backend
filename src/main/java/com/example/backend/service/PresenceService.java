package com.example.backend.service;

import com.example.backend.dto.ParticipantDto;
import com.example.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionMeta {
        private UUID roomId;
        private UUID userId;
        private String username;
        private String displayName;
    }

    // Direct registry mapping WebSocket sessionId -> (roomId, userId, username, displayName)
    private final Map<String, SessionMeta> sessionRegistry = new ConcurrentHashMap<>();

    // In-memory fallback map: roomId -> (userId -> ParticipantDto)
    private final Map<UUID, Map<UUID, ParticipantDto>> localPresenceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> localPresenceExpiry = new ConcurrentHashMap<>();

    private static final String PRESENCE_KEY_PREFIX = "room:presence:";
    private static final long HEARTBEAT_TTL_SECONDS = 35;

    public void registerSession(String sessionId, UUID roomId, UUID userId, String username, String displayName) {
        if (sessionId != null && roomId != null && userId != null) {
            sessionRegistry.put(sessionId, SessionMeta.builder()
                    .roomId(roomId)
                    .userId(userId)
                    .username(username)
                    .displayName(displayName)
                    .build());
        }
    }

    public SessionMeta unregisterSession(String sessionId) {
        return sessionId != null ? sessionRegistry.remove(sessionId) : null;
    }

    public SessionMeta getSession(String sessionId) {
        return sessionId != null ? sessionRegistry.get(sessionId) : null;
    }

    public void recordHeartbeat(UUID roomId, ParticipantDto participant) {
        if (participant == null || participant.getUserId() == null) {
            return;
        }
        participant.setOnline(true);
        UUID userId = participant.getUserId();

        // 1. In-memory storage with 35-second TTL
        localPresenceMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, participant);
        localPresenceExpiry.put(userId, System.currentTimeMillis() + (HEARTBEAT_TTL_SECONDS * 1000));

        // 2. Redis storage: individual key with 35s TTL + set of active user IDs for the room
        try {
            String userKey = PRESENCE_KEY_PREFIX + roomId + ":user:" + userId;
            String setKey = PRESENCE_KEY_PREFIX + roomId + ":users";

            redisTemplate.opsForValue().set(userKey, participant, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
            redisTemplate.opsForSet().add(setKey, userId.toString());
            redisTemplate.expire(setKey, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("Redis presence write fallback to local: {}", e.getMessage());
        }
    }

    public void removeUser(UUID roomId, UUID userId) {
        if (userId == null) return;

        // 1. Remove from local memory
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
        }
        localPresenceExpiry.remove(userId);

        // 2. Remove from Redis
        try {
            String userKey = PRESENCE_KEY_PREFIX + roomId + ":user:" + userId;
            String setKey = PRESENCE_KEY_PREFIX + roomId + ":users";

            redisTemplate.delete(userKey);
            redisTemplate.opsForSet().remove(setKey, userId.toString());
        } catch (Exception e) {
            log.debug("Redis presence remove fallback to local: {}", e.getMessage());
        }
    }

    public void removeRoom(UUID roomId) {
        if (roomId == null) return;
        localPresenceMap.remove(roomId);
        try {
            String setKey = PRESENCE_KEY_PREFIX + roomId + ":users";
            Set<Object> members = redisTemplate.opsForSet().members(setKey);
            if (members != null) {
                for (Object m : members) {
                    redisTemplate.delete(PRESENCE_KEY_PREFIX + roomId + ":user:" + m);
                }
            }
            redisTemplate.delete(setKey);
        } catch (Exception e) {
            log.debug("Redis presence removeRoom fallback: {}", e.getMessage());
        }
    }

    public List<ParticipantDto> getActiveParticipants(UUID roomId) {
        Map<UUID, ParticipantDto> result = new LinkedHashMap<>();
        long now = System.currentTimeMillis();

        // 1. Check local cache (prune any expired)
        Map<UUID, ParticipantDto> roomUsers = localPresenceMap.get(roomId);
        if (roomUsers != null) {
            Iterator<Map.Entry<UUID, ParticipantDto>> it = roomUsers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ParticipantDto> entry = it.next();
                Long expiry = localPresenceExpiry.get(entry.getKey());
                if (expiry != null && expiry > now) {
                    ParticipantDto p = entry.getValue();
                    p.setOnline(true);
                    result.put(p.getUserId(), p);
                } else {
                    it.remove();
                    localPresenceExpiry.remove(entry.getKey());
                }
            }
        }

        // 2. Check Redis individual user keys
        try {
            String setKey = PRESENCE_KEY_PREFIX + roomId + ":users";
            Set<Object> userIds = redisTemplate.opsForSet().members(setKey);
            if (userIds != null && !userIds.isEmpty()) {
                for (Object idObj : userIds) {
                    String uidStr = idObj.toString();
                    String userKey = PRESENCE_KEY_PREFIX + roomId + ":user:" + uidStr;
                    Object cached = redisTemplate.opsForValue().get(userKey);
                    if (cached != null) {
                        if (cached instanceof ParticipantDto p) {
                            p.setOnline(true);
                            result.put(p.getUserId(), p);
                        } else if (cached instanceof Map<?, ?> m) {
                            try {
                                Object idVal = m.get("userId");
                                UUID uId = idVal != null ? UUID.fromString(idVal.toString()) : UUID.fromString(uidStr);
                                String dName = m.get("displayName") != null ? m.get("displayName").toString() : null;
                                String aUrl = m.get("avatarUrl") != null ? m.get("avatarUrl").toString() : null;
                                ParticipantDto p = new ParticipantDto(uId, dName, aUrl, Role.EDITOR, true);
                                result.put(uId, p);
                            } catch (Exception ignored) {}
                        }
                    } else {
                        // Key has expired in Redis, prune from set
                        redisTemplate.opsForSet().remove(setKey, uidStr);
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
        for (ParticipantDto p : getActiveParticipants(roomId)) {
            if (p != null && p.getUserId() != null) {
                activeIds.add(p.getUserId());
            }
        }
        return activeIds;
    }
}
