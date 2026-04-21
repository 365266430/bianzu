package com.bianzu.bianzu_backend.model.dto;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.FormationParadigm;
import com.bianzu.bianzu_backend.model.WeaponType;
import lombok.Data;
import java.util.List;

/**
 * 动态编组请求DTO
 * 前端选择武器、敌方目标后发起编组请求
 */
@Data
public class DynamicFormationRequestDTO {

    private List<String> selectedWeaponTypes;
    private List<String> selectedEnemyIds;
    private FormationParadigm paradigm;
    private List<ProtectionZone> zones;
    private List<EnemyNode> enemyNodes;
    private List<WeaponType> weaponTypes;
    private List<FireType> fireTypes;
    private List<EnemyType> enemyTypes;

    //匹配约束配置--后续可更改弹药阈值、或重新设计
    private FormationConstraints constraints = new FormationConstraints();

    @Data
    public static class FormationConstraints {
        /** 是否必须弹药充足（默认false，允许弹药不足） */
        private Boolean requireAmmoSufficiency = false;

        /** 弹药充足阈值（0.0-1.0），低于此值视为不足 */
        private Double ammoThreshold = 0.3;

        /** 是否必须火力匹配（默认true） */
        private Boolean requireFirepowerMatch = true;

        /** 是否考虑调度成本（默认true） */
        private Boolean considerDispatchCost = true;

        /** 调度成本权重（0.0-1.0） */
        private Double dispatchCostWeight = 0.2;

        /** 是否考虑敌我位置关系（默认true） */
        private Boolean considerPositionRelation = true;

        /** 是否优先拦截已进入保护区的目标（默认true） */
        private Boolean prioritizeInZoneEnemies = true;

        /** 最小拦截概率阈值（低于此值不分配） */
        private Double minInterceptionRate = 0.3;

        /** 最大编组武器数量（默认6） */
        private Integer maxGroupSize = 6;
    }
    private DynamicAlgorithmConfigDTO config;

    // Backward-compatible alias for legacy code still using selectedWeaponIds.
    public List<String> getSelectedWeaponIds() {
        return selectedWeaponTypes;
    }

    public void setSelectedWeaponIds(List<String> selectedWeaponIds) {
        this.selectedWeaponTypes = selectedWeaponIds;
    }
}
