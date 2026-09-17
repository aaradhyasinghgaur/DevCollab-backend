CREATE TABLE room_snapshots (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    revision    INTEGER     NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_snapshot_revision CHECK (revision >= 0)
);

-- for reconnect: find the latest snapshot for a room quickly
CREATE INDEX idx_room_snapshots_room_revision ON room_snapshots(room_id, revision DESC);
