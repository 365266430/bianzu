package com.bianzu.bianzu_backend.processor;

import com.bianzu.bianzu_backend.model.*;
import com.bianzu.bianzu_backend.service.FireTypeService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final double EARTH_RADIUS_KM = 6371D;

    @Autowired
    private FireTypeService fireTypeService;

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
        List<ProtectionZone> safeZones = safeList(zones);
        Map<String, ProtectionZone> zoneByWeaponId = buildZoneByWeaponId(safeZones);
        Map<String, FireType> fireTypeMap = fireTypeService.getFireTypes().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != null)
                .collect(Collectors.toMap(FireType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));

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
        List<WeaponFireAssignment> candidates = matchWeaponFireEnemy(availableWeapons, validEnemies, fireTypeMap, zoneByWeaponId);

        // 4. 敌我位置关系分析
        Map<String, Boolean> inZoneStatus = analyzeInZoneStatus(validEnemies, safeZones);
        Map<String, EnemyNode> enemyById = validEnemies.stream()
                .collect(Collectors.toMap(EnemyNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        // 5. 生成武器-目标分配方案（调用 DQN 算法）
        List<WeaponFireAssignment> assignments = generateAssignments(candidates, paradigm, inZoneStatus, enemyById, fireTypeMap, zoneByWeaponId);

        // 6. 更新武器状态并扣减弹药
        applyAssignments(weapons, assignments);
    }

    /**
     * 弹药自动匹配
     * 遍历武器的所有弹药类型，选出射程和射高匹配且弹药充足的目标组合
     */
    private List<WeaponFireAssignment> matchWeaponFireEnemy(
            List<WeaponNode> weapons,
            List<EnemyNode> enemies,
            Map<String, FireType> fireTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {
        List<WeaponFireAssignment> candidates = new ArrayList<>();

        for (WeaponNode weapon : weapons) {
            if (weapon.getAmmoStates() == null) continue;

            for (EnemyNode enemy : enemies) {
                for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
                    // 弹药不足跳过
                    if (ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) continue;

                    // 检查射程和射高
                    if (!isInRange(weapon, enemy, ammo.getFireUnitType(), fireTypeMap, zoneByWeaponId)) continue;
                    if (!isInAltitude(enemy, ammo.getFireUnitType(), fireTypeMap)) continue;

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
    private boolean isInRange(WeaponNode weapon,
                              EnemyNode enemy,
                              String fireType,
                              Map<String, FireType> fireTypeMap,
                              Map<String, ProtectionZone> zoneByWeaponId) {
        FireType fireTypeInfo = fireTypeMap.get(fireType);
        if (fireTypeInfo == null) {
            return false;
        }
        double maxRangeKm = defaultDouble(fireTypeInfo.getMaxRange()) / 1000D;
        if (maxRangeKm <= 0D) {
            return true;
        }
        double minRangeKm = Math.max(defaultDouble(fireTypeInfo.getMinRange()) / 1000D, 0D);
        Double distanceKm = computeDistanceKm(zoneByWeaponId.get(weapon.getId()), enemy);
        return distanceKm == null || (distanceKm >= minRangeKm && distanceKm <= maxRangeKm);
    }

    /**
     * 检查目标是否在武器射高内
     */
    private boolean isInAltitude(EnemyNode enemy, String fireType, Map<String, FireType> fireTypeMap) {
        if (enemy == null || enemy.getAltitude() == null) {
            return false;
        }
        FireType fireTypeInfo = fireTypeMap.get(fireType);
        if (fireTypeInfo == null) {
            return false;
        }
        double minAlt = Math.max(defaultDouble(fireTypeInfo.getMinAlt()), 0D);
        double maxAlt = fireTypeInfo.getMaxAlt() == null ? Double.MAX_VALUE : Math.max(defaultDouble(fireTypeInfo.getMaxAlt()), minAlt);
        return enemy.getAltitude() >= minAlt && enemy.getAltitude() <= maxAlt;
    }

    /**
     * 分析敌方目标是否在保护区内
     */
    private Map<String, Boolean> analyzeInZoneStatus(
            List<EnemyNode> enemies, List<ProtectionZone> zones) {
        List<ProtectionZone> safeZones = safeList(zones);
        Map<String, Boolean> inZoneStatus = new HashMap<>();
        for (EnemyNode enemy : enemies) {
            boolean inZone = safeZones.stream().anyMatch(zone -> isEnemyInZone(enemy, zone));
            inZoneStatus.put(enemy.getId(), inZone);
        }
        return inZoneStatus;
    }

    /**
     * 判断敌方目标是否在保护区内
     */
    private boolean isEnemyInZone(EnemyNode enemy, ProtectionZone zone) {
        Double distanceKm = computeDistanceKm(zone, enemy);
        if (distanceKm == null) {
            return false;
        }
        double radiusKm = Math.max(defaultDouble(zone.getSize()) / 1000D, 0D);
        return radiusKm > 0D && distanceKm <= radiusKm;
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
            List<WeaponFireAssignment> candidates,
            FormationParadigm paradigm,
            Map<String, Boolean> inZoneStatus,
            Map<String, EnemyNode> enemyById,
            Map<String, FireType> fireTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        double inZoneWeight = paradigm == FormationParadigm.ALL ? 0.26D : 0.2D;

        List<ScoredAssignment> scored = new ArrayList<>();
        for (WeaponFireAssignment candidate : candidates) {
            FireType fireType = fireTypeMap.get(candidate.getFireType());
            if (fireType == null) {
                continue;
            }
            EnemyNode enemy = enemyById.get(candidate.getEnemyId());
            Double distanceKm = computeDistanceKm(zoneByWeaponId.get(candidate.getWeaponId()), enemy);
            double rangeKm = Math.max(defaultDouble(fireType.getMaxRange()) / 1000D, 1D);
            double distanceScore = distanceKm == null ? 0.55D : clamp(1D - distanceKm / rangeKm, 0D, 1D);
            double interceptionScore = clamp(defaultDouble(fireType.getInterception()), 0D, 1D);
            double inZoneScore = Boolean.TRUE.equals(inZoneStatus.get(candidate.getEnemyId())) ? 1D : 0D;
            double score = clamp(interceptionScore * 0.52D + distanceScore * 0.28D + inZoneScore * inZoneWeight, 0D, 1D);
            scored.add(new ScoredAssignment(candidate, score));
        }

        scored.sort(Comparator.comparingDouble(ScoredAssignment::score).reversed());
        Set<String> usedWeapons = new HashSet<>();
        Set<String> usedEnemies = new HashSet<>();
        List<WeaponFireAssignment> assignments = new ArrayList<>();
        for (ScoredAssignment scoredAssignment : scored) {
            WeaponFireAssignment candidate = scoredAssignment.assignment();
            if (usedWeapons.contains(candidate.getWeaponId()) || usedEnemies.contains(candidate.getEnemyId())) {
                continue;
            }
            usedWeapons.add(candidate.getWeaponId());
            usedEnemies.add(candidate.getEnemyId());
            assignments.add(candidate);
        }
        return assignments;
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

    private Map<String, ProtectionZone> buildZoneByWeaponId(List<ProtectionZone> zones) {
        Map<String, ProtectionZone> zoneByWeaponId = new HashMap<>();
        for (ProtectionZone zone : zones) {
            for (String weaponId : safeList(zone.getStationedWeaponIds())) {
                zoneByWeaponId.put(weaponId, zone);
            }
        }
        return zoneByWeaponId;
    }

    private Double computeDistanceKm(ProtectionZone zone, EnemyNode enemy) {
        if (zone == null || enemy == null || zone.getLocation() == null || zone.getLocation().size() < 2
                || enemy.getLatitude() == null || enemy.getLongitude() == null) {
            return null;
        }
        double lon1 = zone.getLocation().get(0);
        double lat1 = zone.getLocation().get(1);
        double lon2 = enemy.getLongitude();
        double lat2 = enemy.getLatitude();
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2D) * Math.sin(dLat / 2D)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2D) * Math.sin(dLon / 2D);
        return EARTH_RADIUS_KM * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private double defaultDouble(Number value) {
        return value == null ? 0D : value.doubleValue();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ScoredAssignment(WeaponFireAssignment assignment, double score) {
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
