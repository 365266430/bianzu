package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnExperience;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnScoredAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DqnTrainingService {

    @Autowired
    private DqnReplayBuffer replayBuffer;

    @Autowired
    private DqnRewardCalculator rewardCalculator;

    @Autowired
    private DqnNeuralQModel qModel;

    public void observeImmediate(DqnScoredAction scoredAction, boolean invalidAction, double learningRate, int batchSize) {
        if (scoredAction == null || scoredAction.getFeatureVector() == null) {
            return;
        }
        double reward = rewardCalculator.estimateImmediateReward(scoredAction.getFeatureVector(), invalidAction);
        DqnExperience experience = new DqnExperience(
                scoredAction.getFeatureVector(),
                scoredAction.getAction(),
                reward,
                null,
                true);
        replayBuffer.add(experience);
        trainBatch(Math.max(batchSize, 1), learningRate, 0.95D);
    }

    public double trainBatch(int batchSize, double learningRate, double gamma) {
        List<DqnExperience> batch = replayBuffer.sample(batchSize);
        if (batch.isEmpty()) {
            return 0D;
        }

        double totalAbsError = 0D;
        for (DqnExperience experience : batch) {
            double reward = experience.getReward() == null ? 0D : experience.getReward();
            boolean done = Boolean.TRUE.equals(experience.getDone()) || experience.getNextState() == null;
            double target = done
                    ? reward
                    : reward + clamp(gamma, 0D, 1D) * qModel.predict(experience.getNextState());
            totalAbsError += Math.abs(qModel.train(experience.getState(), target, learningRate));
        }
        return totalAbsError / batch.size();
    }

    public int replaySize() {
        return replayBuffer.size();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
