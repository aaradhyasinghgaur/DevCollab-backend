CREATE TABLE execution_results (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID        NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    code        TEXT        NOT NULL,
    language    VARCHAR(30) NOT NULL,
    stdout      TEXT        NULL,
    stderr      TEXT        NULL,
    exit_code   INTEGER     NOT NULL,
    duration_ms INTEGER     NOT NULL,
    executed_at TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_exec_language    CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST')),
    CONSTRAINT chk_duration_ms      CHECK (duration_ms >= 0)
);

-- for run history panel: latest runs in a room
CREATE INDEX idx_execution_results_room_time ON execution_results(room_id, executed_at DESC);
CREATE INDEX idx_execution_results_user_id   ON execution_results(user_id);
