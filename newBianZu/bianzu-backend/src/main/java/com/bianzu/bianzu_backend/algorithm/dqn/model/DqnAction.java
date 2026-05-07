package com.bianzu.bianzu_backend.algorithm.dqn.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DQN action for dynamic formation assignment.
 * One action means assigning one weapon and one fire type to one enemy target.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DqnAction {

    private String weaponId;
    private String weaponType;
    private String fireType;
    private String enemyId;
    private String enemyType;
    private String deployDomain;
    private String sourceZoneId;

    public static DqnAction stop() {
        return new DqnAction("NO_OP", null, null, "STOP", null, null, null);
    }
}
