CREATE TABLE rooms (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    invite_code VARCHAR(8)      NOT NULL UNIQUE,
    language    VARCHAR(30)     NOT NULL DEFAULT 'JAVASCRIPT',
    content     TEXT            NOT NULL DEFAULT '',
    revision    INTEGER         NOT NULL DEFAULT 0,
    owner_id    UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_language CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST')),
    CONSTRAINT chk_revision  CHECK (revision >= 0)
);

CREATE INDEX idx_rooms_owner_id    ON rooms(owner_id);
CREATE INDEX idx_rooms_invite_code ON rooms(invite_code);
CREATE INDEX idx_rooms_is_active   ON rooms(is_active);

CREATE TRIGGER trg_rooms_updated_at
    BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
