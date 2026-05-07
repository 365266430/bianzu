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

    @Autowired
    private DqnModelPersistenceService modelPersistenceService;

    private int trainStep = 0;
    private DqnScoredAction lastEpisodeAction;

    public void observeImmediate(DqnScoredAction scoredAction, boolean invalidAction, double learningRate, int batchSize) {
        observeImmediate(scoredAction, invalidAction, learningRate, batchSize, 10);
    }

    public void observeImmediate(DqnScoredAction scoredAction, boolean invalidAction, double learningRate, int batchSize, int targetUpdateFreq) {
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
        trainBatch(Math.max(batchSize, 1), learningRate, 0.95D, targetUpdateFreq);
    }

    public synchronized void observeEpisodeStep(
            DqnScoredAction currentAction,
            boolean invalidAction,
            double learningRate,
            int batchSize,
            double gamma,
            int targetUpdateFreq) {
        if (currentAction == null || currentAction.getFeatureVector() == null) {
            return;
        }
        if (lastEpisodeAction != null && lastEpisodeAction.getFeatureVector() != null) {
            double reward = rewardCalculator.estimateImmediateReward(lastEpisodeAction.getFeatureVector(), invalidAction);
            DqnExperience experience = new DqnExperience(
                    lastEpisodeAction.getFeatureVector(),
                    lastEpisodeAction.getAction(),
                    reward,
                    currentAction.getFeatureVector(),
                    false);
            replayBuffer.add(experience);
            trainBatch(Math.max(batchSize, 1), learningRate, gamma, targetUpdateFreq);
        }
        lastEpisodeAction = currentAction;
    }

    public synchronized void finishEpisode(double learningRate, int batchSize, int targetUpdateFreq) {
        if (lastEpisodeAction == null || lastEpisodeAction.getFeatureVector() == null) {
            return;
        }
        observeImmediate(lastEpisodeAction, false, learningRate, batchSize, targetUpdateFreq);
        lastEpisodeAction = null;
    }

    public double trainBatch(int batchSize, double learningRate, double gamma) {
        return trainBatch(batchSize, learningRate, gamma, 10);
    }

    public double trainBatch(int batchSize, double learningRate, double gamma, int targetUpdateFreq) {
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
                    : reward + clamp(gamma, 0D, 1D) * qModel.predictTarget(experience.getNextState());
            totalAbsError += Math.abs(qModel.train(experience.getState(), target, learningRate));
            trainStep++;
            int syncFreq = Math.max(targetUpdateFreq, 1);
            if (trainStep % syncFreq == 0) {
                qModel.syncTargetNetwork();
                modelPersistenceService.saveModel();
            }
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
