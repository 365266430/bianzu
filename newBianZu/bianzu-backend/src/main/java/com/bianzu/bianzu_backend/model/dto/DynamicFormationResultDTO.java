package com.bianzu.bianzu_backend.model.dto;

import com.bianzu.bianzu_backend.model.FormationParadigm;
import lombok.Data;
import java.util.List;

/**
 * 动态编组结果DTO
 */
@Data
public class DynamicFormationResultDTO {

    private String algorithmType;
    private String recommendedPlanId;
    private FormationParadigm paradigm;

    private Integer enemyCount;
    private Integer weaponCount;
    private Integer totalAmmo;
    private Integer totalChannels;
    private Double estimatedCost;
    private List<DynamicFormationPlanDTO> plans;

    @Data
    public static class DynamicFormationPlanDTO {
        private String planId;
        private String planName;
        private Boolean feasible;
        private Double fitnessScore;

        // 编组信息
        private Integer groupSize;
        private Integer allocatedEnemyCount;
        private List<String> participatingDomains;

        // 警告信息
        private List<String> warnings;

        // 分配详情
        private List<DynamicAllocationDetail> details;
    }

    @Data
    public static class DynamicAllocationDetail {
        // 武器信息
        private String weaponNodeId;
        private String weaponType;
        private String fireType;
        private String deployDomain;

        // 目标信息
        private String targetEnemyId;
        private String targetEnemyType;

        // 位置关系
        private String sourceZoneId;
        private Boolean targetInZone;
        private Double distanceKm;

        // 匹配评估
        private Double assignmentScore;
        private Double interceptionRate;
        private Boolean ammoSufficient;
        private Integer ammoBefore;
        private Integer ammoAfter;
        private Double dispatchCost;
    }
}
