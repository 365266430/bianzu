package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnModelSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class DqnNeuralQModel {

    private static final List<String> FEATURE_NAMES = List.of(
            "enemyCount",
            "availableWeaponCount",
            "totalAmmo",
            "zoneCount",
            "enemyAltitude",
            "enemySpeed",
            "enemyHeadingSin",
            "enemyHeadingCos",
            "enemyValue",
            "enemyDamage",
            "enemyAttackRange",
            "enemyRcs",
            "weaponIdle",
            "ammoCount",
            "ammoRatio",
            "channelCount",
            "interception",
            "maxRange",
            "minRange",
            "maxAlt",
            "minAlt",
            "fireCost",
            "attCost",
            "distanceKm",
            "rangeMatched",
            "rangeProximity",
            "altitudeMatched",
            "targetInZone",
            "zoneValue",
            "zoneHealth",
            "dispatchCost",
            "threatScore"
    );

    private static final int INPUT_SIZE = FEATURE_NAMES.size();
    private static final int HIDDEN_SIZE = 32;

    private final double[][] inputHiddenWeights = new double[HIDDEN_SIZE][INPUT_SIZE];
    private final double[] hiddenBias = new double[HIDDEN_SIZE];
    private final double[] hiddenOutputWeights = new double[HIDDEN_SIZE];
    private double outputBias = 0D;
    private final double[][] targetInputHiddenWeights = new double[HIDDEN_SIZE][INPUT_SIZE];
    private final double[] targetHiddenBias = new double[HIDDEN_SIZE];
    private final double[] targetHiddenOutputWeights = new double[HIDDEN_SIZE];
    private double targetOutputBias = 0D;

    public DqnNeuralQModel() {
        Random random = new Random(42L);
        double inputScale = Math.sqrt(2D / INPUT_SIZE);
        double hiddenScale = Math.sqrt(2D / HIDDEN_SIZE);

        for (int h = 0; h < HIDDEN_SIZE; h++) {
            for (int i = 0; i < INPUT_SIZE; i++) {
                inputHiddenWeights[h][i] = (random.nextDouble() * 2D - 1D) * inputScale;
            }
            hiddenOutputWeights[h] = (random.nextDouble() * 2D - 1D) * hiddenScale;
        }
        syncTargetNetwork();
    }

    public synchronized double predict(DqnFeatureVector vector) {
        ForwardPass pass = forward(toInput(vector), inputHiddenWeights, hiddenBias, hiddenOutputWeights, outputBias);
        return pass.output();
    }

    public synchronized double predictTarget(DqnFeatureVector vector) {
        ForwardPass pass = forward(toInput(vector), targetInputHiddenWeights, targetHiddenBias, targetHiddenOutputWeights, targetOutputBias);
        return pass.output();
    }

    public synchronized void syncTargetNetwork() {
        for (int h = 0; h < HIDDEN_SIZE; h++) {
            System.arraycopy(inputHiddenWeights[h], 0, targetInputHiddenWeights[h], 0, INPUT_SIZE);
            targetHiddenBias[h] = hiddenBias[h];
            targetHiddenOutputWeights[h] = hiddenOutputWeights[h];
        }
        targetOutputBias = outputBias;
    }

    public synchronized double train(DqnFeatureVector vector, double target, double learningRate) {
        double[] input = toInput(vector);
        ForwardPass pass = forward(input, inputHiddenWeights, hiddenBias, hiddenOutputWeights, outputBias);
        double clippedTarget = clamp(target, 0D, 1D);
        double prediction = pass.output();
        double error = clippedTarget - prediction;
        double lr = clamp(learningRate, 0.00001D, 0.05D);

        double outputDelta = error * sigmoidDerivative(prediction);
        for (int h = 0; h < HIDDEN_SIZE; h++) {
            double oldHiddenOutputWeight = hiddenOutputWeights[h];
            hiddenOutputWeights[h] += lr * outputDelta * pass.hidden()[h];
            double hiddenDelta = outputDelta * oldHiddenOutputWeight * reluDerivative(pass.hiddenRaw()[h]);
            for (int i = 0; i < INPUT_SIZE; i++) {
                inputHiddenWeights[h][i] += lr * hiddenDelta * input[i];
            }
            hiddenBias[h] += lr * hiddenDelta;
        }
        outputBias += lr * outputDelta;
        return error;
    }

    public List<String> featureNames() {
        return FEATURE_NAMES;
    }

    public synchronized DqnModelSnapshot snapshot() {
        DqnModelSnapshot snapshot = new DqnModelSnapshot();
        snapshot.setFeatureNames(FEATURE_NAMES);
        snapshot.setInputHiddenWeights(copyMatrix(inputHiddenWeights));
        snapshot.setHiddenBias(hiddenBias.clone());
        snapshot.setHiddenOutputWeights(hiddenOutputWeights.clone());
        snapshot.setOutputBias(outputBias);
        snapshot.setTargetInputHiddenWeights(copyMatrix(targetInputHiddenWeights));
        snapshot.setTargetHiddenBias(targetHiddenBias.clone());
        snapshot.setTargetHiddenOutputWeights(targetHiddenOutputWeights.clone());
        snapshot.setTargetOutputBias(targetOutputBias);
        return snapshot;
    }

    public synchronized boolean restore(DqnModelSnapshot snapshot) {
        if (snapshot == null
                || snapshot.getInputHiddenWeights() == null
                || snapshot.getInputHiddenWeights().length != HIDDEN_SIZE
                || snapshot.getHiddenBias() == null
                || snapshot.getHiddenBias().length != HIDDEN_SIZE
                || snapshot.getHiddenOutputWeights() == null
                || snapshot.getHiddenOutputWeights().length != HIDDEN_SIZE) {
            return false;
        }
        if (!copyInto(snapshot.getInputHiddenWeights(), inputHiddenWeights)) {
            return false;
        }
        System.arraycopy(snapshot.getHiddenBias(), 0, hiddenBias, 0, HIDDEN_SIZE);
        System.arraycopy(snapshot.getHiddenOutputWeights(), 0, hiddenOutputWeights, 0, HIDDEN_SIZE);
        outputBias = snapshot.getOutputBias();

        if (snapshot.getTargetInputHiddenWeights() != null
                && snapshot.getTargetHiddenBias() != null
                && snapshot.getTargetHiddenOutputWeights() != null
                && copyInto(snapshot.getTargetInputHiddenWeights(), targetInputHiddenWeights)
                && snapshot.getTargetHiddenBias().length == HIDDEN_SIZE
                && snapshot.getTargetHiddenOutputWeights().length == HIDDEN_SIZE) {
            System.arraycopy(snapshot.getTargetHiddenBias(), 0, targetHiddenBias, 0, HIDDEN_SIZE);
            System.arraycopy(snapshot.getTargetHiddenOutputWeights(), 0, targetHiddenOutputWeights, 0, HIDDEN_SIZE);
            targetOutputBias = snapshot.getTargetOutputBias();
        } else {
            syncTargetNetwork();
        }
        return true;
    }

    private ForwardPass forward(
            double[] input,
            double[][] firstLayerWeights,
            double[] firstLayerBias,
            double[] secondLayerWeights,
            double secondLayerBias) {
        double[] hiddenRaw = new double[HIDDEN_SIZE];
        double[] hidden = new double[HIDDEN_SIZE];

        for (int h = 0; h < HIDDEN_SIZE; h++) {
            double value = firstLayerBias[h];
            for (int i = 0; i < INPUT_SIZE; i++) {
                value += firstLayerWeights[h][i] * input[i];
            }
            hiddenRaw[h] = value;
            hidden[h] = relu(value);
        }

        double outputRaw = secondLayerBias;
        for (int h = 0; h < HIDDEN_SIZE; h++) {
            outputRaw += secondLayerWeights[h] * hidden[h];
        }
        return new ForwardPass(hiddenRaw, hidden, sigmoid(outputRaw));
    }

    private double[] toInput(DqnFeatureVector vector) {
        double[] input = new double[INPUT_SIZE];
        for (int i = 0; i < INPUT_SIZE; i++) {
            input[i] = vector == null ? 0D : clamp(vector.get(FEATURE_NAMES.get(i)), -1D, 1D);
        }
        return input;
    }

    private double relu(double value) {
        return Math.max(0D, value);
    }

    private double reluDerivative(double value) {
        return value > 0D ? 1D : 0D;
    }

    private double sigmoid(double value) {
        if (value >= 35D) {
            return 1D;
        }
        if (value <= -35D) {
            return 0D;
        }
        return 1D / (1D + Math.exp(-value));
    }

    private double sigmoidDerivative(double sigmoidOutput) {
        return sigmoidOutput * (1D - sigmoidOutput);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double[][] copyMatrix(double[][] source) {
        double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private boolean copyInto(double[][] source, double[][] target) {
        if (source.length != target.length) {
            return false;
        }
        for (int row = 0; row < target.length; row++) {
            if (source[row] == null || source[row].length != target[row].length) {
                return false;
            }
            System.arraycopy(source[row], 0, target[row], 0, target[row].length);
        }
        return true;
    }

    private record ForwardPass(double[] hiddenRaw, double[] hidden, double output) {
    }
}
