package com.bianzu.bianzu_backend.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * 静态编队结果数据传输对象
 */

@Data
public class StaticFormationResultDTO {
    private String algorithmType;
    private String recommendedPlanId;
    private Integer enemyCount;
    private Integer weaponCount;
    private Integer zoneCount;
    private Integer totalAmmo;
    private Integer totalChannels;
    private List<String> supportedParadigms = new ArrayList<>();
    private List<DomainResourceSummary> domainResourceSummaries = new ArrayList<>();
    private List<FirepowerResourceSummary> firepowerResourceSummaries = new ArrayList<>();
    private List<FormationPlanDTO> plans = new ArrayList<>();

    @Data
    public static class DomainResourceSummary {
        private String deployDomain;
        private Integer weaponNodeCount;
        private Integer ammoCount;
        private Integer channelCount;
    }

    @Data
    public static class FirepowerResourceSummary {
        private String deployDomain;
        private String weaponType;
        private String fireType;
        private Integer weaponNodeCount;
        private Integer ammoCount;
        private Integer channelCount;
        private Double averageInterceptionRate;
        private Double maxRange;
    }
}
