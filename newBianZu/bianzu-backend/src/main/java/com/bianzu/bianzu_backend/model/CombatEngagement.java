package com.bianzu.bianzu_backend.model;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnScoredAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A transient engagement record produced during a simulation tick.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombatEngagement {

    private String weaponId;
    private String enemyId;
    private String fireType;
    private String sourceZoneId;
    private Double interceptionRate;
    private DqnScoredAction scoredAction;
}
