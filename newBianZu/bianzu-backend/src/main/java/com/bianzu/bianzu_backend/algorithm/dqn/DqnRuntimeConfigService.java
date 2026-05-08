package com.bianzu.bianzu_backend.algorithm.dqn;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class DqnRuntimeConfigService {

    public enum Mode {
        TRAIN,
        INFER
    }

    private Mode mode = Mode.TRAIN;
    private Instant updatedAt = Instant.now();

    public synchronized Mode mode() {
        return mode;
    }

    public synchronized boolean isTrainingEnabled() {
        return mode == Mode.TRAIN;
    }

    public synchronized double effectiveEpsilon(double configuredEpsilon) {
        return mode == Mode.INFER ? 0D : clamp(configuredEpsilon, 0D, 1D);
    }

    public synchronized Map<String, Object> setMode(String requestedMode) {
        if (requestedMode == null || requestedMode.isBlank()) {
            throw new IllegalArgumentException("DQN mode cannot be empty");
        }
        mode = Mode.valueOf(requestedMode.trim().toUpperCase());
        updatedAt = Instant.now();
        return status();
    }

    public synchronized Map<String, Object> status() {
        return Map.of(
                "mode", mode.name(),
                "trainingEnabled", mode == Mode.TRAIN,
                "epsilonForcedToZero", mode == Mode.INFER,
                "updatedAt", updatedAt.toString()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
