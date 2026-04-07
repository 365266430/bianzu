package com.bianzu.bianzu_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AlgorithmConfigDTO {
    private String algorithmType;

    private Double distanceWeight;
    private Double firepowerWeight;
    private Double defenseWeight;

    private Integer maxGroupSize;
    private Integer maxIterations;
    private Integer particleCount;
    private Integer planCount;

    private List<String> allowedWeaponTypes;
    private List<String> selectedParadigms;
}
