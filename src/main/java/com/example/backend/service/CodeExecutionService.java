package com.example.backend.service;

import com.example.backend.dto.ExecutionResponse;
import com.example.backend.entity.ExecutionResult;
import com.example.backend.entity.ProgrammingLanguage;
import com.example.backend.entity.Room;
import com.example.backend.entity.User;
import com.example.backend.respository.ExecutionResultRepo;
import com.example.backend.respository.RoomRepo;
import com.example.backend.respository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {

    private final ExecutionResultRepo executionResultRepo;
    private final RoomRepo roomRepo;
    private final UserRepo userRepo;
    private final SimpMessageSendingOperations messagingTemplate;

    private static final int TIMEOUT_SECONDS = 6;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public ExecutionResponse executeCode(UUID roomId, String code, ProgrammingLanguage language, String stdin, String username) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        User user = userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        long startTime = System.currentTimeMillis();
        ExecutionResultDto runResult = runInSandbox(code, language, stdin);
        long duration = System.currentTimeMillis() - startTime;

        ExecutionResult resultEntity = ExecutionResult.builder()
                .room(room)
                .user(user)
                .code(code)
                .language(language)
                .stdout(runResult.stdout)
                .stderr(runResult.stderr)
                .exitCode(runResult.exitCode)
                .durationMs((int) duration)
                .build();

        ExecutionResult saved = executionResultRepo.save(resultEntity);

        ExecutionResponse response = ExecutionResponse.builder()
                .id(saved.getId())
                .roomId(roomId)
                .language(language)
                .stdout(saved.getStdout())
                .stderr(saved.getStderr())
                .exitCode(saved.getExitCode())
                .durationMs(saved.getDurationMs())
                .executedAt(saved.getExecutionAt())
                .build();

        // Broadcast to all room peers
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/execution", response);

        return response;
    }

    public List<ExecutionResponse> getRoomExecutions(UUID roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        List<ExecutionResult> results = executionResultRepo.findByRoomOrderByExecutionAtDesc(room);
        return results.stream().map(r -> ExecutionResponse.builder()
                .id(r.getId())
                .roomId(roomId)
                .language(r.getLanguage())
                .stdout(r.getStdout())
                .stderr(r.getStderr())
                .exitCode(r.getExitCode())
                .durationMs(r.getDurationMs())
                .executedAt(r.getExecutionAt())
                .build()
        ).toList();
    }

    private ExecutionResultDto runInSandbox(String code, ProgrammingLanguage language, String stdin) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("devcollab_exec_");

            return switch (language) {
                case JAVASCRIPT -> executeNode(tempDir, code, stdin);
                case PYTHON -> executePython(tempDir, code, stdin);
                case JAVA -> executeJava(tempDir, code, stdin);
                case CPP -> executeCpp(tempDir, code, stdin);
                case C -> executeC(tempDir, code, stdin);
                default -> executeGeneric(tempDir, code, language, stdin);
            };

        } catch (Exception e) {
            log.error("Execution error: ", e);
            return new ExecutionResultDto("", "Execution system error: " + e.getMessage(), 1);
        } finally {
            if (tempDir != null) {
                deleteDirSilently(tempDir);
            }
        }
    }

    private ExecutionResultDto executeNode(Path dir, String code, String stdin) throws Exception {
        Path file = dir.resolve("index.js");
        Files.writeString(file, code, StandardCharsets.UTF_8);
        return runProcess(dir, List.of("node", file.toAbsolutePath().toString()), stdin);
    }

    private ExecutionResultDto executePython(Path dir, String code, String stdin) throws Exception {
        Path file = dir.resolve("script.py");
        Files.writeString(file, code, StandardCharsets.UTF_8);

        String pythonCmd = isWindows() ? "python" : "python3";
        return runProcess(dir, List.of(pythonCmd, file.toAbsolutePath().toString()), stdin);
    }

    private ExecutionResultDto executeJava(Path dir, String code, String stdin) throws Exception {
        Path file = dir.resolve("Main.java");
        Files.writeString(file, code, StandardCharsets.UTF_8);

        // Compile
        ExecutionResultDto compile = runProcess(dir, List.of("javac", "Main.java"), null);
        if (compile.exitCode != 0) {
            return compile;
        }

        // Run
        return runProcess(dir, List.of("java", "Main"), stdin);
    }

    private ExecutionResultDto executeCpp(Path dir, String code, String stdin) throws Exception {
        Path file = dir.resolve("main.cpp");
        Files.writeString(file, code, StandardCharsets.UTF_8);

        String binary = isWindows() ? "main.exe" : "./main";
        ExecutionResultDto compile = runProcess(dir, List.of("g++", "-O2", "main.cpp", "-o", binary), null);
        if (compile.exitCode != 0) {
            return compile;
        }

        return runProcess(dir, List.of(dir.resolve(binary).toAbsolutePath().toString()), stdin);
    }

    private ExecutionResultDto executeC(Path dir, String code, String stdin) throws Exception {
        Path file = dir.resolve("main.c");
        Files.writeString(file, code, StandardCharsets.UTF_8);

        String binary = isWindows() ? "main.exe" : "./main";
        ExecutionResultDto compile = runProcess(dir, List.of("gcc", "-O2", "main.c", "-o", binary), null);
        if (compile.exitCode != 0) {
            return compile;
        }

        return runProcess(dir, List.of(dir.resolve(binary).toAbsolutePath().toString()), stdin);
    }

    private ExecutionResultDto executeGeneric(Path dir, String code, ProgrammingLanguage language, String stdin) {
        return new ExecutionResultDto("", "Execution not configured for language: " + language, 1);
    }

    private ExecutionResultDto runProcess(Path workingDir, List<String> command, String stdin) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());

            Process process = pb.start();

            if (stdin != null && !stdin.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (IOException ignored) {}
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ExecutionResultDto("", "Execution timed out (exceeded " + TIMEOUT_SECONDS + " seconds).", 124);
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            return new ExecutionResultDto(stdout, stderr, process.exitValue());

        } catch (IOException e) {
            return new ExecutionResultDto("", "Runtime command unavailable or failed to launch (" + command.get(0) + "): " + e.getMessage(), 127);
        } catch (Exception e) {
            return new ExecutionResultDto("", "Error during execution: " + e.getMessage(), 1);
        }
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
            if (buffer.size() >= MAX_OUTPUT_BYTES) {
                buffer.write("\n... [Output truncated at 64KB]".getBytes(StandardCharsets.UTF_8));
                break;
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void deleteDirSilently(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }

    private record ExecutionResultDto(String stdout, String stderr, int exitCode) {}
}
