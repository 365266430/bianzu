package com.bianzu.bianzu_backend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class FormationPlanDTO {
    private String planId;
    private String paradigm; // 联合范式：空空联合, 空地联合, 地地联合
    private Double fitnessScore; // 算法适应度
    private List<AllocationDetail> details; // 具体火力分配详情

    @Data
    public static class AllocationDetail {
        private String weaponNodeId;
        private String targetEnemyId;
        private String deployDomain; // 冗余字段，方便前端判断是"地"还是"空"
    }
}
