-- 1. Add description column to rooms if not present
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS description TEXT;

-- 2. Drop old check constraints on rooms.language and re-add with all languages
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_language_check;
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS chk_language;
ALTER TABLE rooms ADD CONSTRAINT chk_language CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST'));

-- 3. Drop old check constraints on room_participants.role and re-add
ALTER TABLE room_participants DROP CONSTRAINT IF EXISTS room_participants_role_check;
ALTER TABLE room_participants DROP CONSTRAINT IF EXISTS chk_role;
ALTER TABLE room_participants ADD CONSTRAINT chk_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'));

-- 4. Drop old check constraints on execution_results.language and re-add
ALTER TABLE execution_results DROP CONSTRAINT IF EXISTS execution_results_language_check;
ALTER TABLE execution_results DROP CONSTRAINT IF EXISTS chk_exec_language;
ALTER TABLE execution_results ADD CONSTRAINT chk_exec_language CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST'));
