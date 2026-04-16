package com.bianzu.bianzu_backend.model.dto;
import lombok.Data;
/**
 * 动态编组算法配置
 * 适配 DQN（深度强化学习）算法参数
 */
@Data
public class DynamicAlgorithmConfigDTO {

    private String algorithmType = "DQN";

    //DQN 核心参数--后续修改
    /** 学习率（默认0.001） */
    private Double learningRate = 0.001;

    /** 折扣因子（默认0.95） */
    private Double gamma = 0.95;

    /** 探索率（默认0.1） */
    private Double epsilon = 0.1;

    /** 目标网络更新频率（默认每10步更新） */
    private Integer targetUpdateFreq = 10;

    /** 经验回放缓冲区大小（默认10000） */
    private Integer replayBufferSize = 10000;

    //训练参数
    /** 训练轮次（默认100） */
    private Integer epochs = 100;

    /** 每轮步数（默认200） */
    private Integer stepsPerEpoch = 200;

    /** 批大小（默认32） */
    private Integer batchSize = 32;

    // 同静态的权重参数
    private Double distanceWeight = 0.34;
    private Double firepowerWeight = 0.38;
    private Double defenseWeight = 0.28;

    /** 最大编组数量（默认6） */
    private Integer maxGroupSize = 6;

    /** 生成方案数量（默认3） */
    private Integer planCount = 3;
}
