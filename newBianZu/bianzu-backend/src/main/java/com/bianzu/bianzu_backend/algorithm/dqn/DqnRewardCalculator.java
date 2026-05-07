package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import org.springframework.stereotype.Component;

@Component
public class DqnRewardCalculator {

    public double estimateImmediateReward(DqnFeatureVector vector, boolean invalidAction) {
        if (vector == null) {
            return invalidAction ? -2D : 0D;
        }
        double reward =
                2.0D * vector.get("enemyValue") * vector.get("interception")
                        + 1.5D * vector.get("targetInZone")
                        + 1.0D * vector.get("threatScore")
                        - 0.5D * vector.get("fireCost")
                        - 0.5D * vector.get("dispatchCost");
        if (invalidAction) {
            reward -= 2D;
        }
        return reward;
    }
}
