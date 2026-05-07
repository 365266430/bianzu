package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DqnLinearQModel {

    private final Map<String, Double> weights = new ConcurrentHashMap<>();
    private double bias = 0D;

    public DqnLinearQModel() {
        weights.put("interception", 0.28D);
        weights.put("threatScore", 0.24D);
        weights.put("rangeProximity", 0.16D);
        weights.put("ammoRatio", 0.10D);
        weights.put("targetInZone", 0.10D);
        weights.put("zoneValue", 0.06D);
        weights.put("zoneHealth", 0.04D);
        weights.put("rangeMatched", 0.04D);
        weights.put("altitudeMatched", 0.04D);
        weights.put("dispatchCost", -0.12D);
        weights.put("fireCost", -0.04D);
    }

    public synchronized double predict(DqnFeatureVector vector) {
        return clamp(predictRaw(vector), 0D, 1D);
    }

    public synchronized double train(DqnFeatureVector vector, double target, double learningRate) {
        if (vector == null) {
            return 0D;
        }
        double prediction = predictRaw(vector);
        double error = target - prediction;
        double lr = clamp(learningRate, 0.00001D, 0.2D);

        for (Map.Entry<String, Double> feature : vector.getFeatures().entrySet()) {
            String name = feature.getKey();
            double value = feature.getValue() == null ? 0D : feature.getValue();
            double current = weights.getOrDefault(name, 0D);
            weights.put(name, current + lr * error * value);
        }
        bias += lr * error;
        return error;
    }

    public synchronized Map<String, Double> snapshotWeights() {
        return Map.copyOf(weights);
    }

    private double predictRaw(DqnFeatureVector vector) {
        if (vector == null) {
            return 0D;
        }
        double value = bias;
        for (Map.Entry<String, Double> feature : vector.getFeatures().entrySet()) {
            value += weights.getOrDefault(feature.getKey(), 0D) * feature.getValue();
        }
        return value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
