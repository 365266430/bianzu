package com.bianzu.bianzu_backend.model.dto;

import lombok.Data;

@Data
public class AlgorithmConfigDTO {
    private String algorithmType;

    private Double distanceWeight;//距离权重
    private Double firepowerWeight;//火力权重
    private Double defenseWeight;//防御价值权重

    private Integer maxGroupSize; // 最大编组规模
    private Integer maxIterations; // 针对启发式算法的最大迭代次数
    private Integer particleCount; // 粒子数量

}
