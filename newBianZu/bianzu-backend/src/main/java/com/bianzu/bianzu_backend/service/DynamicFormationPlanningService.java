package com.bianzu.bianzu_backend.service;

import com.bianzu.bianzu_backend.model.*;
import com.bianzu.bianzu_backend.model.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 动态编组规划服务
 * 职责：根据实时态势，动态进行武器-目标分配
 */
@Slf4j
@Service
public class DynamicFormationPlanningService {

    /**
     * 生成动态编组方案
     */
    public DynamicFormationResultDTO generateDynamicFormation(DynamicFormationRequestDTO request) {
        log.info("开始动态编组 | 武器数: {} | 敌方目标数: {} | 范式: {}",
                request.getSelectedWeaponIds().size(),
                request.getSelectedEnemyIds().size(),
                request.getParadigm().getChineseName());

        // 1. 过滤可用武器（待命+弹药充足）
        List<WeaponNode> availableWeapons = filterAvailableWeapons(request);

        // 2. 过滤有效目标
        List<EnemyNode> validEnemies = filterValidEnemies(request);

        if (availableWeapons.isEmpty() || validEnemies.isEmpty()) {
            log.warn("无可用武器或有效目标");
            return buildEmptyResult(request, availableWeapons, validEnemies);
        }

        // 3. 弹药充足性检查
        Map<String, Boolean> ammoSufficiency = checkAmmoSufficiency(availableWeapons, request.getConstraints());

        // 4. 火力匹配验证
        Map<String, List<EnemyNode>> candidates = matchWeaponEnemy(availableWeapons, validEnemies);

        // 5. 调度成本计算
        Map<String, Double> dispatchCosts = calculateDispatchCosts(candidates, request.getZones());

        // 6. 敌我位置关系分析
        Map<String, Boolean> inZoneStatus = analyzeInZoneStatus(validEnemies, request.getZones());

        // 7. 生成方案
        DynamicFormationResultDTO result = buildResult(
                availableWeapons, validEnemies, candidates, dispatchCosts, inZoneStatus, ammoSufficiency, request);

        return result;
    }

    
    //私有方法
    /**
     * 过滤可用武器（状态为待命 且 弹药充足）
     */
    private List<WeaponNode> filterAvailableWeapons(DynamicFormationRequestDTO request) {
        return request.getWeaponNodes().stream()
                .filter(w -> request.getSelectedWeaponIds().contains(w.getId()))
                .filter(w -> w.getStatus() == 0) // 0=待命
                .filter(this::hasAmmo)
                .toList();
    }

    /**
     * 过滤有效目标
     */
    private List<EnemyNode> filterValidEnemies(DynamicFormationRequestDTO request) {
        return request.getEnemyNodes().stream()
                .filter(e -> request.getSelectedEnemyIds().contains(e.getId()))
                .filter(e -> e.getAltitude() != null) // 未被击毁
                .toList();
    }

    /**
     * 检查弹药是否充足
     */
    private Map<String, Boolean> checkAmmoSufficiency(
            List<WeaponNode> weapons, DynamicFormationRequestDTO.FormationConstraints constraints) {
        Map<String, Boolean> result = new HashMap<>();
        boolean requireSufficiency = Boolean.TRUE.equals(constraints.getRequireAmmoSufficiency());
        double threshold = constraints.getAmmoThreshold() != null ? constraints.getAmmoThreshold() : 0.3;

        for (WeaponNode weapon : weapons) {
            boolean sufficient = isAmmoSufficient(weapon, threshold);
            if (requireSufficiency && !sufficient) {
                result.put(weapon.getId(), false);
            } else {
                result.put(weapon.getId(), true);
            }
        }
        return result;
    }

    /**
     * 判断武器弹药是否充足
     */
    private boolean isAmmoSufficient(WeaponNode weapon, double threshold) {
        if (weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return false;
        }
        int total = weapon.getAmmoStates().stream()
                .mapToInt(ammo -> ammo.getCurrentCount() != null ? ammo.getCurrentCount() : 0)
                .sum();
        // 简化：弹药数量 > 0 即视为充足
        return total > 0;
    }

    /**
     * 火力匹配：每种武器能打哪些目标
     */
    private Map<String, List<EnemyNode>> matchWeaponEnemy(List<WeaponNode> weapons, List<EnemyNode> enemies) {
        Map<String, List<EnemyNode>> candidates = new HashMap<>();
        for (WeaponNode weapon : weapons) {
            List<EnemyNode> matched = enemies.stream()
                    .filter(enemy -> isInRange(weapon, enemy))
                    .filter(enemy -> isInAltitude(weapon, enemy))
                    .toList();
            candidates.put(weapon.getId(), matched);
        }
        return candidates;
    }

    private boolean isInRange(WeaponNode weapon, EnemyNode enemy) {
        // TODO: 根据 FireType.maxRange 和实际距离计算
        return true;
    }

    private boolean isInAltitude(WeaponNode weapon, EnemyNode enemy) {
        // TODO: 根据 FireType.minAlt/maxAlt 判断
        return true;
    }

    /**
     * 计算调度成本
     */
    private Map<String, Double> calculateDispatchCosts(
            Map<String, List<EnemyNode>> candidates, List<ProtectionZone> zones) {
        Map<String, Double> costs = new HashMap<>();
        for (Map.Entry<String, List<EnemyNode>> entry : candidates.entrySet()) {
            String weaponId = entry.getKey();
            List<EnemyNode> targets = entry.getValue();
            if (!targets.isEmpty()) {
                // TODO: 根据距离 * attCost 计算调度成本
                costs.put(weaponId, 0.0);
            }
        }
        return costs;
    }

    /**
     * 分析敌方目标是否在保护区内
     */
    private Map<String, Boolean> analyzeInZoneStatus(List<EnemyNode> enemies, List<ProtectionZone> zones) {
        Map<String, Boolean> inZoneStatus = new HashMap<>();
        for (EnemyNode enemy : enemies) {
            boolean inZone = zones.stream().anyMatch(zone -> isEnemyInZone(enemy, zone));
            inZoneStatus.put(enemy.getId(), inZone);
        }
        return inZoneStatus;
    }

    private boolean isEnemyInZone(EnemyNode enemy, ProtectionZone zone) {
        // TODO: 根据经纬度和保护区坐标计算距离，判断是否在半径内
        return false;
    }

    /**
     * 检查武器弹药是否充足
     */
    private boolean hasAmmo(WeaponNode weapon) {
        if (weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return false;
        }
        return weapon.getAmmoStates().stream()
                .anyMatch(ammo -> ammo.getCurrentCount() != null && ammo.getCurrentCount() > 0);
    }

    /**
     * 构建结果
     */
    private DynamicFormationResultDTO buildResult(
            List<WeaponNode> availableWeapons,
            List<EnemyNode> validEnemies,
            Map<String, List<EnemyNode>> candidates,
            Map<String, Double> dispatchCosts,
            Map<String, Boolean> inZoneStatus,
            Map<String, Boolean> ammoSufficiency,
            DynamicFormationRequestDTO request) {

        DynamicFormationResultDTO result = new DynamicFormationResultDTO();
        result.setAlgorithmType(request.getConfig() != null ? request.getConfig().getAlgorithmType() : "DQN");
        result.setParadigm(request.getParadigm());
        result.setEnemyCount(validEnemies.size());
        result.setWeaponCount(availableWeapons.size());
        result.setTotalAmmo(0); // TODO: 计算实际消耗
        result.setTotalChannels(0); // TODO: 计算实际通道
        result.setEstimatedCost(0.0); // TODO: 计算实际成本

        // TODO: 生成方案列表（调用 DQN 算法）
        result.setPlans(new ArrayList<>());

        return result;
    }

    /**
     * 构建空结果
     */
    private DynamicFormationResultDTO buildEmptyResult(
            DynamicFormationRequestDTO request,
            List<WeaponNode> availableWeapons,
            List<EnemyNode> validEnemies) {

        DynamicFormationResultDTO result = new DynamicFormationResultDTO();
        result.setAlgorithmType(request.getConfig() != null ? request.getConfig().getAlgorithmType() : "DQN");
        result.setParadigm(request.getParadigm());
        result.setEnemyCount(validEnemies.size());
        result.setWeaponCount(availableWeapons.size());
        result.setTotalAmmo(0);
        result.setTotalChannels(0);
        result.setEstimatedCost(0.0);
        result.setPlans(new ArrayList<>());

        return result;
    }
}
