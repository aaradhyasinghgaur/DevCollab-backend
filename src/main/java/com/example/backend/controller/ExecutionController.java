package com.example.backend.controller;

import com.example.backend.dto.ExecuteCodeRequest;
import com.example.backend.dto.ExecutionResponse;
import com.example.backend.service.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class ExecutionController {

    private final CodeExecutionService codeExecutionService;

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeCode(
            @PathVariable UUID roomId,
            @RequestBody ExecuteCodeRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        ExecutionResponse response = codeExecutionService.executeCode(
                roomId,
                request.getCode(),
                request.getLanguage(),
                request.getStdin(),
                username
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionResponse>> getRoomExecutions(@PathVariable UUID roomId) {
        List<ExecutionResponse> history = codeExecutionService.getRoomExecutions(roomId);
        return ResponseEntity.ok(history);
    }
}
