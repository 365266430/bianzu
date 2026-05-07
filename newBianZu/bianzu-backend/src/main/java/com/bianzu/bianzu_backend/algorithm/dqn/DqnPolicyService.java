package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnScoredAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DqnPolicyService {

    @Autowired
    private DqnNeuralQModel qModel;

    /**
     * 对候选动作进行DQN策略评分，计算其Q值。
     * <p>
     * 该方法结合启发式评分和特征向量评分，通过加权融合计算出最终的Q值。
     * 当前实现使用固定的权重系数（启发式0.55，特征评分0.45），未来可替换为训练好的神经网络模型。
     * </p>
     * @param candidate 待评分的候选动作对象，包含启发式评分和特征向量信息，方法会直接修改该对象的Q值字段
     * @return 已设置Q值的候选动作对象，与输入参数为同一实例
     */
    public DqnScoredAction score(DqnScoredAction candidate) {
        double heuristicScore = candidate.getHeuristicScore() == null ? 0D : candidate.getHeuristicScore();
        double learnedScore = qModel.predict(candidate.getFeatureVector());
        candidate.setQValue(clamp(heuristicScore * 0.25D + learnedScore * 0.75D, 0D, 1D));
        return candidate;
    }

    public List<DqnScoredAction> scoreAll(List<DqnScoredAction> candidates) {
        return candidates.stream()
                .map(this::score)
                .sorted((left, right) -> Double.compare(
                        right.getQValue() == null ? 0D : right.getQValue(),
                        left.getQValue() == null ? 0D : left.getQValue()))
                .toList();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
