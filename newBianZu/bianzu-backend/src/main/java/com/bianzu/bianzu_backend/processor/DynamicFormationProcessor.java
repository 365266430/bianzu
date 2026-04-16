package com.bianzu.bianzu_backend.processor;

import com.bianzu.bianzu_backend.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态编组处理器
 * 每帧执行武器-目标分配决策
 */
@Slf4j
@Component
public class DynamicFormationProcessor implements SimulationProcessor {

    @Override
    public void process(SimulationContext context) {
        log.debug("动态编组处理 | step: {} | 敌方: {} | 武器: {} | 范式: {}",
                context.getStep(),
                context.getEnemies().size(),
                context.getWeapons().size(),
                context.getParadigm());

        List<EnemyNode> enemies = context.getEnemies();
        List<WeaponNode> weapons = context.getWeapons();
        List<ProtectionZone> zones = context.getZones();
        FormationParadigm paradigm = context.getParadigm();

        // 1. 过滤可用武器（状态为待命、弹药充足）
        List<WeaponNode> availableWeapons = weapons.stream()
                .filter(w -> w.getStatus() == 0) // 0=待命
                .filter(this::hasAmmo)
                .toList();

        // 2. 过滤有效目标（未被击毁）
        List<EnemyNode> validEnemies = enemies.stream()
                .filter(e -> e.getAltitude() != null)
                .toList();

        if (availableWeapons.isEmpty() || validEnemies.isEmpty()) {
            log.debug("无可用武器或有效目标，跳过编组");
            return;
        }

        // 3. 弹药自动匹配：遍历武器的所有弹药，选出射程和射高匹配且弹药充足的目标组合
        List<WeaponFireAssignment> candidates = matchWeaponFireEnemy(availableWeapons, validEnemies);

        // 4. 敌我位置关系分析
        Map<String, Boolean> inZoneStatus = analyzeInZoneStatus(validEnemies, zones);

        // 5. 生成武器-目标分配方案（调用 DQN 算法）
        List<WeaponFireAssignment> assignments = generateAssignments(candidates, paradigm);

        // 6. 更新武器状态并扣减弹药
        applyAssignments(weapons, assignments);
    }

    /**
     * 弹药自动匹配
     * 遍历武器的所有弹药类型，选出射程和射高匹配且弹药充足的目标组合
     */
    private List<WeaponFireAssignment> matchWeaponFireEnemy(
            List<WeaponNode> weapons, List<EnemyNode> enemies) {
        List<WeaponFireAssignment> candidates = new ArrayList<>();

        for (WeaponNode weapon : weapons) {
            if (weapon.getAmmoStates() == null) continue;

            for (EnemyNode enemy : enemies) {
                for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
                    // 弹药不足跳过
                    if (ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) continue;

                    // 检查射程和射高
                    if (!isInRange(weapon, enemy, ammo.getFireUnitType())) continue;
                    if (!isInAltitude(weapon, enemy, ammo.getFireUnitType())) continue;

                    // 自动匹配成功：武器用该弹药打目标
                    candidates.add(new WeaponFireAssignment(
                            weapon.getId(),
                            enemy.getId(),
                            ammo.getFireUnitType()
                    ));
                }
            }
        }
        return candidates;
    }

    /**
     * 检查目标是否在武器射程内
     */
    private boolean isInRange(WeaponNode weapon, EnemyNode enemy, String fireType) {
        // TODO: 根据 FireType.maxRange 和实际距离计算
        return true;
    }

    /**
     * 检查目标是否在武器射高内
     */
    private boolean isInAltitude(WeaponNode weapon, EnemyNode enemy, String fireType) {
        // TODO: 根据 FireType.minAlt/maxAlt 判断
        return true;
    }

    /**
     * 分析敌方目标是否在保护区内
     */
    private Map<String, Boolean> analyzeInZoneStatus(
            List<EnemyNode> enemies, List<ProtectionZone> zones) {
        Map<String, Boolean> inZoneStatus = new HashMap<>();
        for (EnemyNode enemy : enemies) {
            boolean inZone = zones.stream().anyMatch(zone -> isEnemyInZone(enemy, zone));
            inZoneStatus.put(enemy.getId(), inZone);
        }
        return inZoneStatus;
    }

    /**
     * 判断敌方目标是否在保护区内
     */
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
     * 生成武器-目标分配（调用 DQN 算法）
     */
    private List<WeaponFireAssignment> generateAssignments(
            List<WeaponFireAssignment> candidates, FormationParadigm paradigm) {
        // TODO: 调用 DQN 算法进行最优分配
        // 简化：返回所有候选作为分配
        return candidates;
    }

    /**
     * 应用分配结果：更新武器状态并扣减弹药
     */
    private void applyAssignments(List<WeaponNode> weapons, List<WeaponFireAssignment> assignments) {
        // 按武器分组
        Map<String, List<WeaponFireAssignment>> byWeapon = assignments.stream()
                .collect(Collectors.groupingBy(WeaponFireAssignment::getWeaponId));

        for (WeaponNode weapon : weapons) {
            List<WeaponFireAssignment> weaponAssignments = byWeapon.get(weapon.getId());
            if (weaponAssignments != null && !weaponAssignments.isEmpty()) {
                weapon.setStatus(1); // 1=分配中
                // 扣减弹药（每种弹药扣1发）
                for (WeaponFireAssignment assignment : weaponAssignments) {
                    decrementAmmo(weapon, assignment.getFireType());
                }
            }
        }
    }

    /**
     * 扣减弹药
     */
    private void decrementAmmo(WeaponNode weapon, String fireType) {
        if (weapon.getAmmoStates() == null) return;
        for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
            if (fireType.equals(ammo.getFireUnitType())
                    && ammo.getCurrentCount() != null
                    && ammo.getCurrentCount() > 0) {
                ammo.setCurrentCount(ammo.getCurrentCount() - 1);
                break;
            }
        }
    }

    /**
     * 武器-弹药-目标分配记录
     * 记录某个武器用某种弹药打某个目标
     */
    @Data
    @AllArgsConstructor
    private static class WeaponFireAssignment {
        private String weaponId;
        private String enemyId;
        private String fireType; // 自动匹配的弹药类型
    }
}
