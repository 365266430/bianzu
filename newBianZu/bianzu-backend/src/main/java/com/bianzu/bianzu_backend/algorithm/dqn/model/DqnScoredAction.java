package com.bianzu.bianzu_backend.algorithm.dqn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Candidate action with extracted features and a Q value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DqnScoredAction {

    private DqnAction action;
    private DqnFeatureVector featureVector;
    private Double heuristicScore;
    private Double qValue;
}
