package com.bianzu.bianzu_backend.algorithm.dqn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Replay-buffer sample shape for later online DQN training.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DqnExperience {

    private DqnFeatureVector state;
    private DqnAction action;
    private Double reward;
    private DqnFeatureVector nextState;
    private Boolean done;
}
