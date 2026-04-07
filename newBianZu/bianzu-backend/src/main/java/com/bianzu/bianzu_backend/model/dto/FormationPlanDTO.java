package com.bianzu.bianzu_backend.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * 编队方案数据传输对象
 */
@Data
public class FormationPlanDTO {
    private String planId;
    private String planName;
    private String paradigm;
    private Boolean feasible;
    private String summary;
    private Double fitnessScore;
    private Double distanceScore;
    private Double firepowerScore;
    private Double defenseScore;
    private Double coverageScore;
    private Double expectedInterceptionRate;
    private Double estimatedCost;
    private Integer groupSize;
    private Integer allocatedEnemyCount;
    private List<String> participatingDomains = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<AllocationDetail> details = new ArrayList<>();

    @Data
    public static class AllocationDetail {
        private String weaponNodeId;
        private String weaponType;
        private String fireType;
        private String targetEnemyId;
        private String targetEnemyType;
        private String sourceZoneId;
        private String deployDomain;
        private Double distanceKm;
        private Double assignmentScore;
        private Double estimatedInterceptionRate;
    }
}
