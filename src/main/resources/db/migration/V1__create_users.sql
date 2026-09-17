CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NULL,
    display_name    VARCHAR(100)    NOT NULL,
    avatar_url      VARCHAR(500)    NULL,
    provider        VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    provider_id     VARCHAR(255)    NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_provider CHECK (provider IN ('LOCAL', 'GOOGLE', 'GITHUB')),
    CONSTRAINT chk_password  CHECK (
        (provider = 'LOCAL' AND password_hash IS NOT NULL) OR
        (provider != 'LOCAL' AND password_hash IS NULL)
    )
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);

-- auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
