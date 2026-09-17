CREATE TABLE room_participants (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         VARCHAR(10) NOT NULL DEFAULT 'EDITOR',
    joined_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_room_user  UNIQUE (room_id, user_id),
    CONSTRAINT chk_role      CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE INDEX idx_room_participants_room_id ON room_participants(room_id);
CREATE INDEX idx_room_participants_user_id ON room_participants(user_id);
-- for presence: find all participants active in last N seconds
CREATE INDEX idx_room_participants_last_seen ON room_participants(room_id, last_seen_at);
