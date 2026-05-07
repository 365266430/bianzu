package com.bianzu.bianzu_backend.algorithm.dqn.model;

import lombok.Data;

import java.util.List;

@Data
public class DqnModelSnapshot {

    private List<String> featureNames;
    private double[][] inputHiddenWeights;
    private double[] hiddenBias;
    private double[] hiddenOutputWeights;
    private double outputBias;
    private double[][] targetInputHiddenWeights;
    private double[] targetHiddenBias;
    private double[] targetHiddenOutputWeights;
    private double targetOutputBias;
}
