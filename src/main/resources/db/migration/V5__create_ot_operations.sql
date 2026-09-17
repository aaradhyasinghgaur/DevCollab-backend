CREATE TABLE ot_operations (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    revision        INTEGER     NOT NULL,
    operation_data  TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_ot_revision CHECK (revision >= 0)
);

-- most critical index in the whole schema
-- every OT transform query hits: WHERE room_id = ? AND revision > ?
CREATE INDEX idx_ot_operations_room_revision ON ot_operations(room_id, revision);
CREATE INDEX idx_ot_operations_user_id       ON ot_operations(user_id);
