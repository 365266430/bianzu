package com.bianzu.bianzu_backend.algorithm.dqn;

import com.alibaba.fastjson2.JSON;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DqnTrainingLogService {

    private static final Path LOG_PATH = Path.of("models", "dqn-training-log.jsonl");

    public synchronized void append(String event, Map<String, Object> payload) {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", Instant.now().toString());
            row.put("event", event);
            row.putAll(payload == null ? Map.of() : payload);
            Files.writeString(
                    LOG_PATH,
                    JSON.toJSONString(row) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    Files.exists(LOG_PATH)
                            ? new java.nio.file.OpenOption[] { java.nio.file.StandardOpenOption.APPEND }
                            : new java.nio.file.OpenOption[] { java.nio.file.StandardOpenOption.CREATE });
        } catch (IOException e) {
            System.err.println("Append DQN training log failed: " + e.getMessage());
        }
    }

    public synchronized Map<String, Object> status() {
        try {
            boolean exists = Files.exists(LOG_PATH);
            return Map.of(
                    "path", LOG_PATH.toAbsolutePath().toString(),
                    "exists", exists,
                    "sizeBytes", exists ? Files.size(LOG_PATH) : 0L,
                    "lastModified", exists ? Files.getLastModifiedTime(LOG_PATH).toInstant().toString() : ""
            );
        } catch (IOException e) {
            return Map.of(
                    "path", LOG_PATH.toAbsolutePath().toString(),
                    "exists", false,
                    "error", e.getMessage()
            );
        }
    }
}
