package com.example.backend.dto;

import com.example.backend.entity.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponse {
    private UUID id;
    private UUID roomId;
    private ProgrammingLanguage language;
    private String stdout;
    private String stderr;
    private int exitCode;
    private int durationMs;
    private LocalDateTime executedAt;
}
