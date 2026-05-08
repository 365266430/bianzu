package com.bianzu.bianzu_backend.algorithm.dqn;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnModelSnapshot;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@Service
public class DqnModelPersistenceService {

    private static final Path MODEL_PATH = Path.of("models", "dqn-model.json");

    @Autowired
    private DqnNeuralQModel qModel;

    @PostConstruct
    public void loadOnStartup() {
        loadModel();
    }

    public synchronized void saveModel() {
        try {
            Files.createDirectories(MODEL_PATH.getParent());
            String json = JSON.toJSONString(qModel.snapshot());
            Files.writeString(MODEL_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Save DQN model failed: " + e.getMessage());
        }
    }

    public synchronized boolean loadModel() {
        if (!Files.exists(MODEL_PATH)) {
            return false;
        }
        try {
            String json = Files.readString(MODEL_PATH, StandardCharsets.UTF_8);
            DqnModelSnapshot snapshot = JSON.parseObject(json, DqnModelSnapshot.class);
            boolean restored = qModel.restore(snapshot);
            if (restored) {
                System.out.println("DQN model loaded: " + MODEL_PATH.toAbsolutePath());
            }
            return restored;
        } catch (Exception e) {
            System.err.println("Load DQN model failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean deleteModel() {
        try {
            return Files.deleteIfExists(MODEL_PATH);
        } catch (IOException e) {
            System.err.println("Delete DQN model failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized Map<String, Object> modelFileStatus() {
        try {
            boolean exists = Files.exists(MODEL_PATH);
            return Map.of(
                    "path", MODEL_PATH.toAbsolutePath().toString(),
                    "exists", exists,
                    "sizeBytes", exists ? Files.size(MODEL_PATH) : 0L,
                    "lastModified", exists ? Files.getLastModifiedTime(MODEL_PATH).toInstant().toString() : "",
                    "checkedAt", Instant.now().toString()
            );
        } catch (IOException e) {
            return Map.of(
                    "path", MODEL_PATH.toAbsolutePath().toString(),
                    "exists", false,
                    "error", e.getMessage(),
                    "checkedAt", Instant.now().toString()
            );
        }
    }
}
