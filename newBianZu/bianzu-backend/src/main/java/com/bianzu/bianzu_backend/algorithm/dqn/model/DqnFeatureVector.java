package com.bianzu.bianzu_backend.algorithm.dqn.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered feature vector for the first DQN scoring layer.
 */
@Data
public class DqnFeatureVector {

    private final Map<String, Double> features = new LinkedHashMap<>();

    public void put(String name, double value) {
        features.put(name, sanitize(value));
    }

    public double get(String name) {
        return features.getOrDefault(name, 0D);
    }

    public List<Double> toList() {
        return new ArrayList<>(features.values());
    }

    private double sanitize(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0D;
        }
        return value;
    }
}
