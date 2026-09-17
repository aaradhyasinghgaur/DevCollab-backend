CREATE TABLE chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_content_not_empty CHECK (LENGTH(TRIM(content)) > 0)
);

-- for loading last N messages ordered by time
CREATE INDEX idx_chat_messages_room_created ON chat_messages(room_id, created_at DESC);
