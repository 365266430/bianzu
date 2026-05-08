package com.bianzu.bianzu_backend.processor;

import com.bianzu.bianzu_backend.algorithm.dqn.DqnFeatureBuilder;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnPolicyService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnRuntimeConfigService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnTrainingService;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnAction;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnScoredAction;
import com.bianzu.bianzu_backend.model.*;
import com.bianzu.bianzu_backend.service.EnemyTypeService;
import com.bianzu.bianzu_backend.service.FireTypeService;
import com.bianzu.bianzu_backend.service.WeaponTypeService;
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

    @Autowired
    private EnemyTypeService enemyTypeService;

    @Autowired
    private WeaponTypeService weaponTypeService;

    @Autowired
    private DqnFeatureBuilder dqnFeatureBuilder;

    @Autowired
    private DqnPolicyService dqnPolicyService;

    @Autowired
    private DqnTrainingService dqnTrainingService;

    @Autowired
    private DqnRuntimeConfigService dqnRuntimeConfigService;

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
        releaseAssignedWeapons(weapons);
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
            dqnTrainingService.finishEpisode(0.001D, 32, 10);
            log.debug("无可用武器或有效目标，跳过编组");
            return;
        }

        // 3. 弹药自动匹配：遍历武器的所有弹药，选出射程和射高匹配且弹药充足的目标组合
        List<WeaponFireAssignment> candidates = matchWeaponFireEnemy(availableWeapons, validEnemies, fireTypeMap, safeZones);
        if (candidates.isEmpty()) {
            dqnTrainingService.finishEpisode(0.001D, 32, 10);
            return;
        }

        // 4. 敌我位置关系分析
        Map<String, Boolean> inZoneStatus = analyzeInZoneStatus(validEnemies, safeZones);
        Map<String, EnemyNode> enemyById = validEnemies.stream()
                .collect(Collectors.toMap(EnemyNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        // 5. 生成武器-目标分配方案（增强版：考虑敌方威胁属性）
        List<WeaponFireAssignment> assignments = generateAssignmentsEnhanced(
                candidates, availableWeapons, paradigm, inZoneStatus, enemyById, fireTypeMap, zoneByWeaponId, safeZones, context);

        // 6. 更新武器状态并扣减弹药
        applyAssignments(weapons, safeZones, assignments);
    }

    private void releaseAssignedWeapons(List<WeaponNode> weapons) {
        for (WeaponNode weapon : safeList(weapons)) {
            if (Objects.equals(weapon.getStatus(), 1)) {
                weapon.setStatus(0);
            }
        }
    }

    /**
     * 弹药自动匹配
     * 遍历武器的所有弹药类型，选出射程和射高匹配且弹药充足的目标组合
     */
    private List<WeaponFireAssignment> matchWeaponFireEnemy(
            List<WeaponNode> weapons,
            List<EnemyNode> enemies,
            Map<String, FireType> fireTypeMap,
            List<ProtectionZone> zones) {
        List<WeaponFireAssignment> candidates = new ArrayList<>();

        for (WeaponNode weapon : weapons) {
            if (weapon.getAmmoStates() == null) continue;

            for (EnemyNode enemy : enemies) {
                EnemyType enemyType = enemyTypeService.getEnemyType(enemy.getType());
                ProtectionZone threatenedZone = resolveThreatenedZone(enemy, enemyType, zones);
                for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
                    // 弹药不足跳过
                    if (ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) continue;

                    // 检查射程和射高
                    if (!isInRange(enemy, ammo.getFireUnitType(), fireTypeMap, threatenedZone)) continue;
                    if (!isInAltitude(enemy, ammo.getFireUnitType(), fireTypeMap)) continue;

                    // 自动匹配成功：武器用该弹药打目标
                    candidates.add(new WeaponFireAssignment(
                            weapon.getId(),
                            enemy.getId(),
                            threatenedZone == null ? null : threatenedZone.getId(),
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
    private boolean isInRange(EnemyNode enemy,
                              String fireType,
                              Map<String, FireType> fireTypeMap,
                              ProtectionZone sourceZone) {
        FireType fireTypeInfo = fireTypeMap.get(fireType);
        if (fireTypeInfo == null) {
            return false;
        }
        double maxRangeKm = defaultDouble(fireTypeInfo.getMaxRange()) / 1000D;
        if (maxRangeKm <= 0D) {
            return true;
        }
        double minRangeKm = Math.max(defaultDouble(fireTypeInfo.getMinRange()) / 1000D, 0D);
        Double distanceKm = computeDistanceKm(sourceZone, enemy);
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
     * 生成武器-目标分配（增强版：考虑敌方威胁属性）
     * 硬约束：射程、射高、库存充足
     * 软因素：杀伤力、在保护区内、拦截率、距离、库存余量、调度成本
     */
    private List<WeaponFireAssignment> generateAssignmentsEnhanced(
            List<WeaponFireAssignment> candidates,
            List<WeaponNode> availableWeapons,
            FormationParadigm paradigm,
            Map<String, Boolean> inZoneStatus,
            Map<String, EnemyNode> enemyById,
            Map<String, FireType> fireTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId,
            List<ProtectionZone> zones,
            SimulationContext context) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建敌方类型映射（用于获取 damageCapability, maxAttackRange）
        Map<String, EnemyType> enemyTypeById = new HashMap<>();
        for (EnemyNode enemy : enemyById.values()) {
            EnemyType et = enemyTypeService.getEnemyType(enemy.getType());
            if (et != null) {
                enemyTypeById.put(enemy.getId(), et);
            }
        }
        Map<String, WeaponNode> weaponById = safeList(availableWeapons).stream()
                .collect(Collectors.toMap(WeaponNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, WeaponType> weaponTypeMap = weaponTypeService.getWeaponTypes().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != null)
                .collect(Collectors.toMap(WeaponType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));
        int totalAmmo = safeList(availableWeapons).stream()
                .flatMap(weapon -> safeList(weapon.getAmmoStates()).stream())
                .mapToInt(ammo -> ammo.getCurrentCount() == null ? 0 : ammo.getCurrentCount())
                .sum();

        // 构建威胁评分列表
        List<PendingThreatCandidate> pendingCandidates = new ArrayList<>();
        for (WeaponFireAssignment candidate : candidates) {
            FireType fireType = fireTypeMap.get(candidate.getFireType());
            if (fireType == null) continue;

            EnemyNode enemy = enemyById.get(candidate.getEnemyId());
            if (enemy == null) continue;

            EnemyType enemyType = enemyTypeById.get(candidate.getEnemyId());
            ProtectionZone sourceZone = resolveZoneById(candidate.getSourceZoneId(), zones);
            if (sourceZone == null) {
                sourceZone = zoneByWeaponId.get(candidate.getWeaponId());
            }

            // 计算各项评分因子
            double threatScore = computeThreatScore(candidate.getEnemyId(), enemy, enemyType, inZoneStatus, zones);
            double interceptionScore = clamp(defaultDouble(fireType.getInterception()), 0D, 1D);
            Double distanceKm = computeDistanceKm(sourceZone, enemy);
            double rangeKm = Math.max(defaultDouble(fireType.getMaxRange()) / 1000D, 1D);
            double distanceScore = distanceKm == null ? 0.55D : clamp(1D - distanceKm / rangeKm, 0D, 1D);
            double ammoScore = computeAmmoScore(candidate.getWeaponId(), candidate.getFireType(), availableWeapons);
            double costScore = computeCostScore(fireType, distanceKm);

            // 综合评分
            double totalScore = clamp(
                    threatScore * 0.35D +
                    interceptionScore * 0.25D +
                    distanceScore * 0.15D +
                    ammoScore * 0.1D +
                    costScore * 0.15D,
                    0D, 1D);

            WeaponNode weapon = weaponById.get(candidate.getWeaponId());
            WeaponType weaponType = weapon == null ? null : weaponTypeMap.get(weapon.getType());
            DqnAction action = new DqnAction(
                    candidate.getWeaponId(),
                    weapon == null ? null : weapon.getType(),
                    candidate.getFireType(),
                    candidate.getEnemyId(),
                    enemy.getType(),
                    normalizeDomain(weaponType == null ? null : weaponType.getDeployDomain()),
                    sourceZone == null ? "UNASSIGNED_ZONE" : sourceZone.getId());
            DqnFeatureVector featureVector = dqnFeatureBuilder.buildCandidateFeature(
                    action,
                    weapon,
                    weaponType,
                    enemy,
                    enemyType,
                    fireType,
                    sourceZone,
                    Boolean.TRUE.equals(inZoneStatus.get(candidate.getEnemyId())),
                    distanceKm,
                    distanceKm == null ? 0D : distanceKm * Math.max(defaultDouble(fireType.getAttCost()), 0D),
                    enemyById.size(),
                    safeList(availableWeapons).size(),
                    totalAmmo,
                    safeList(zones).size());
            DqnScoredAction scoredAction = new DqnScoredAction(action, featureVector, totalScore, null);
            pendingCandidates.add(new PendingThreatCandidate(scoredAction, candidate, threatScore));
        }

        // 按威胁等级和综合评分排序
        List<DqnScoredAction> rankedActions = dqnPolicyService.scoreAll(
                pendingCandidates.stream().map(PendingThreatCandidate::scoredAction).toList(),
                dqnRuntimeConfigService.effectiveEpsilon(0.1D));
        Map<DqnScoredAction, PendingThreatCandidate> candidateByAction = new IdentityHashMap<>();
        for (PendingThreatCandidate pendingCandidate : pendingCandidates) {
            candidateByAction.put(pendingCandidate.scoredAction(), pendingCandidate);
        }
        List<ThreatScoredAssignment> scored = new ArrayList<>();
        for (DqnScoredAction rankedAction : rankedActions) {
            PendingThreatCandidate pendingCandidate = candidateByAction.get(rankedAction);
            if (pendingCandidate != null) {
                scored.add(new ThreatScoredAssignment(
                        pendingCandidate.assignment(),
                        rankedAction.getQValue() == null ? 0D : rankedAction.getQValue(),
                        pendingCandidate.threatScore(),
                        rankedAction));
            }
        }

        // 贪心分配
        Set<String> usedWeapons = new HashSet<>();
        Set<String> usedEnemies = new HashSet<>();
        List<WeaponFireAssignment> assignments = new ArrayList<>();
        List<CombatEngagement> engagements = new ArrayList<>();

        for (ThreatScoredAssignment s : scored) {
            WeaponFireAssignment candidate = s.assignment();
            if (usedWeapons.contains(candidate.getWeaponId()) || usedEnemies.contains(candidate.getEnemyId())) {
                continue;
            }
            // 检查库存限制：低库存弹药只能用于高威胁目标
            if (!canUseScarceAmmo(candidate.getWeaponId(), candidate.getFireType(), s.threatScore(), availableWeapons)) {
                continue;
            }
            usedWeapons.add(candidate.getWeaponId());
            usedEnemies.add(candidate.getEnemyId());
            assignments.add(candidate);
            FireType fireType = fireTypeMap.get(candidate.getFireType());
            engagements.add(new CombatEngagement(
                    candidate.getWeaponId(),
                    candidate.getEnemyId(),
                    candidate.getFireType(),
                    candidate.getSourceZoneId(),
                    fireType == null ? 0D : clamp(defaultDouble(fireType.getInterception()), 0D, 1D),
                    s.scoredAction()));
        }
        context.setEngagements(engagements);
        return assignments;
    }

    /**
     * 计算敌方威胁评分
     * 因素：是否在保护区内、杀伤力、打击半径
     */
    private double computeThreatScore(String enemyId, EnemyNode enemy, EnemyType enemyType,
                                       Map<String, Boolean> inZoneStatus, List<ProtectionZone> zones) {
        boolean inZone = Boolean.TRUE.equals(inZoneStatus.get(enemyId));
        double damage = enemyType != null ? defaultDouble(enemyType.getDamageCapability()) / 100D : 0.3D;
        double attackRange = enemyType != null ? defaultDouble(enemyType.getMaxAttackRange()) : 0D;

        // 区内基础分 + 杀伤力权重 + 打击半径因子
        double zoneBonus = inZone ? 0.35D : 0D;
        double damageBonus = damage * 0.4D;
        double rangeBonus = (attackRange > 50000) ? 0.1D : (attackRange > 10000) ? 0.05D : 0D;

        return clamp(zoneBonus + damageBonus + rangeBonus, 0D, 1D);
    }

    /**
     * 计算弹药余量评分（库存多加分，库存少扣分）
     */
    private double computeAmmoScore(String weaponId, String fireType, List<WeaponNode> availableWeapons) {
        int currentCount = resolveAmmoCount(weaponId, fireType, availableWeapons);
        if (currentCount <= 0) {
            return -0.15D;
        }
        return clamp(currentCount / 8D, 0.02D, 0.2D);
    }

    /**
     * 计算调度成本评分（成本越低分数越高）
     */
    private double computeCostScore(FireType fireType, Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0D) return 0.1D;
        double attCost = defaultDouble(fireType.getAttCost());
        double costPenalty = clamp(distanceKm * attCost / 100D, 0D, 0.3D);
        return 0.15D - costPenalty;
    }

    /**
     * 检查能否使用稀缺弹药（库存 <= 3）
     * 只有高威胁目标（威胁分 > 0.5）才能使用稀缺弹药
     */
    private boolean canUseScarceAmmo(String weaponId, String fireType, double threatScore, List<WeaponNode> availableWeapons) {
        int currentCount = resolveAmmoCount(weaponId, fireType, availableWeapons);
        if (currentCount > 3) {
            return true;
        }
        return threatScore >= 0.55D;
    }

    private int resolveAmmoCount(String weaponId, String fireType, List<WeaponNode> availableWeapons) {
        for (WeaponNode weapon : safeList(availableWeapons)) {
            if (!Objects.equals(weapon.getId(), weaponId)) {
                continue;
            }
            for (WeaponNode.NodeAmmoState ammo : safeList(weapon.getAmmoStates())) {
                if (Objects.equals(ammo.getFireUnitType(), fireType)) {
                    return ammo.getCurrentCount() == null ? 0 : ammo.getCurrentCount();
                }
            }
        }
        return 0;
    }

        private void applyAssignments(List<WeaponNode> weapons, List<ProtectionZone> zones, List<WeaponFireAssignment> assignments) {
        syncStationedWeaponsByAssignments(zones, assignments);
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

    private ProtectionZone resolveThreatenedZone(EnemyNode enemy, EnemyType enemyType, List<ProtectionZone> zones) {
        ProtectionZone bestZone = null;
        double bestScore = -1D;
        double attackRangeKm = Math.max(defaultDouble(enemyType == null ? null : enemyType.getMaxAttackRange()) / 1000D, 0D);
        for (ProtectionZone zone : safeList(zones)) {
            Double distanceKm = computeDistanceKm(zone, enemy);
            if (distanceKm == null) {
                continue;
            }
            double radiusKm = Math.max(defaultDouble(zone.getSize()) / 1000D, 0D);
            boolean inZone = radiusKm > 0D && distanceKm <= radiusKm;
            boolean inAttackRange = attackRangeKm <= 0D || distanceKm <= attackRangeKm;
            if (!inZone && !inAttackRange) {
                continue;
            }
            double headingScore = computeHeadingScore(enemy, zone);
            double distanceScore = inZone ? 1D : clamp(1D - distanceKm / Math.max(attackRangeKm, 1D), 0D, 1D);
            double valueScore = clamp(defaultDouble(zone.getValue()) / 3D, 0D, 1D);
            double score = (inZone ? 1D : 0D) + distanceScore * 0.45D + headingScore * 0.35D + valueScore * 0.2D;
            if (score > bestScore) {
                bestScore = score;
                bestZone = zone;
            }
        }
        return bestZone;
    }

    private ProtectionZone resolveZoneById(String zoneId, List<ProtectionZone> zones) {
        if (zoneId == null || zoneId.isBlank()) {
            return null;
        }
        for (ProtectionZone zone : safeList(zones)) {
            if (zoneId.equals(zone.getId())) {
                return zone;
            }
        }
        return null;
    }

    private double computeHeadingScore(EnemyNode enemy, ProtectionZone zone) {
        if (enemy == null || zone == null || enemy.getHeading() == null
                || enemy.getLatitude() == null || enemy.getLongitude() == null
                || zone.getLocation() == null || zone.getLocation().size() < 2) {
            return 0.5D;
        }
        double bearing = computeBearingDegrees(
                enemy.getLatitude(),
                enemy.getLongitude(),
                zone.getLocation().get(1),
                zone.getLocation().get(0));
        double diff = Math.abs(((enemy.getHeading() - bearing + 540D) % 360D) - 180D);
        return clamp(1D - diff / 180D, 0D, 1D);
    }

    private double computeBearingDegrees(double fromLat, double fromLon, double toLat, double toLon) {
        double lat1 = Math.toRadians(fromLat);
        double lat2 = Math.toRadians(toLat);
        double deltaLon = Math.toRadians(toLon - fromLon);
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360D) % 360D;
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

    private String normalizeDomain(String rawDomain) {
        if (rawDomain == null) {
            return "UNKNOWN";
        }
        String normalized = rawDomain.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "UNKNOWN";
        }
        if (normalized.contains("air")) {
            return "AIR";
        }
        if (normalized.contains("ground") || normalized.contains("land")) {
            return "GROUND";
        }
        if (normalized.contains("sea") || normalized.contains("naval")) {
            return "SEA";
        }
        if (normalized.contains("space")) {
            return "SPACE";
        }
        return rawDomain.toUpperCase(Locale.ROOT);
    }

    private record ThreatScoredAssignment(
            WeaponFireAssignment assignment,
            double score,
            double threatScore,
            DqnScoredAction scoredAction) {
    }

    private record PendingThreatCandidate(
            DqnScoredAction scoredAction,
            WeaponFireAssignment assignment,
            double threatScore) {
    }

    private void syncStationedWeaponsByAssignments(List<ProtectionZone> zones, List<WeaponFireAssignment> assignments) {
        if (zones == null || zones.isEmpty() || assignments == null || assignments.isEmpty()) {
            return;
        }
        Map<String, ProtectionZone> zoneById = zones.stream()
                .filter(Objects::nonNull)
                .filter(zone -> zone.getId() != null)
                .collect(Collectors.toMap(ProtectionZone::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (ProtectionZone zone : zones) {
            zone.setStationedWeaponIds(new ArrayList<>());
        }
        for (WeaponFireAssignment assignment : assignments) {
            ProtectionZone zone = zoneById.get(assignment.getSourceZoneId());
            if (zone != null) {
                zone.getStationedWeaponIds().add(assignment.getWeaponId());
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
        private String sourceZoneId;
        private String fireType; // 自动匹配的弹药类型
    }
}
