package com.example.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConstraintFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println("[DatabaseConstraintFixer] Running schema fixes for check constraints...");

        try {
            jdbcTemplate.execute("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS description TEXT");
            System.out.println("[DatabaseConstraintFixer] Ensured rooms.description column exists.");
        } catch (Exception e) {
            System.out.println("[DatabaseConstraintFixer] rooms.description check: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_language_check");
            jdbcTemplate.execute("ALTER TABLE rooms DROP CONSTRAINT IF EXISTS chk_language");
            jdbcTemplate.execute("ALTER TABLE rooms ADD CONSTRAINT chk_language CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST'))");
            System.out.println("[DatabaseConstraintFixer] Updated rooms.language check constraint to include JAVASCRIPT.");
        } catch (Exception e) {
            System.out.println("[DatabaseConstraintFixer] rooms.language check: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE room_participants DROP CONSTRAINT IF EXISTS room_participants_role_check");
            jdbcTemplate.execute("ALTER TABLE room_participants DROP CONSTRAINT IF EXISTS chk_role");
            jdbcTemplate.execute("ALTER TABLE room_participants ADD CONSTRAINT chk_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))");
            System.out.println("[DatabaseConstraintFixer] Updated room_participants.role check constraint.");
        } catch (Exception e) {
            System.out.println("[DatabaseConstraintFixer] room_participants.role check: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE execution_results DROP CONSTRAINT IF EXISTS execution_results_language_check");
            jdbcTemplate.execute("ALTER TABLE execution_results DROP CONSTRAINT IF EXISTS chk_exec_language");
            jdbcTemplate.execute("ALTER TABLE execution_results ADD CONSTRAINT chk_exec_language CHECK (language IN ('JAVASCRIPT', 'PYTHON', 'JAVA', 'CPP', 'C', 'GO', 'RUST'))");
            System.out.println("[DatabaseConstraintFixer] Updated execution_results.language check constraint.");
        } catch (Exception e) {
            System.out.println("[DatabaseConstraintFixer] execution_results.language check: " + e.getMessage());
        }
    }
}
