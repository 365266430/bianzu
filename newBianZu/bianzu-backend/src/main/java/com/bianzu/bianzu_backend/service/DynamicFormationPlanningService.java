package com.bianzu.bianzu_backend.service;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.FormationParadigm;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.model.dto.DynamicFormationRequestDTO;
import com.bianzu.bianzu_backend.model.dto.DynamicFormationResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DynamicFormationPlanningService {

    private static final double EARTH_RADIUS_KM = 6371D;

    @Autowired
    private FireTypeService fireTypeService;

    @Autowired
    private WeaponTypeService weaponTypeService;

    /**
     * 生成动态编组方案
     */
    public DynamicFormationResultDTO generateDynamicFormation(DynamicFormationRequestDTO request) {
        List<ProtectionZone> safeZones = safeList(request.getZones());
        List<EnemyNode> safeEnemyNodes = safeList(request.getEnemyNodes());
        List<WeaponType> safeWeaponTypes = safeList(request.getWeaponTypes());
        List<FireType> safeFireTypes = safeList(request.getFireTypes());
        DynamicFormationRequestDTO.FormationConstraints constraints = request.getConstraints() == null
                ? new DynamicFormationRequestDTO.FormationConstraints()
                : request.getConstraints();

        if (safeWeaponTypes.isEmpty()) {
            safeWeaponTypes = safeList(weaponTypeService.getWeaponTypes());
        }
        if (safeFireTypes.isEmpty()) {
            safeFireTypes = safeList(fireTypeService.getFireTypes());
        }

        Map<String, FireType> fireTypeMap = safeFireTypes.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != null)
                .collect(Collectors.toMap(FireType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, WeaponType> weaponTypeMap = safeWeaponTypes.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != null)
                .collect(Collectors.toMap(WeaponType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<String, ProtectionZone> zoneByWeaponId = new LinkedHashMap<>();
        List<WeaponNode> virtualWeapons = buildVirtualWeapons(request, safeZones, weaponTypeMap, zoneByWeaponId);

        log.info("Dynamic formation planning start | selectedWeaponTypes={} | selectedEnemyIds={} | paradigm={}",
                request.getSelectedWeaponTypes() == null ? 0 : request.getSelectedWeaponTypes().size(),
                request.getSelectedEnemyIds() == null ? 0 : request.getSelectedEnemyIds().size(),
                request.getParadigm());

        List<WeaponNode> availableWeapons = filterAvailableWeapons(virtualWeapons);
        List<EnemyNode> validEnemies = filterValidEnemies(request, safeEnemyNodes);

        if (availableWeapons.isEmpty() || validEnemies.isEmpty()) {
            log.warn("无可用武器或有效目标");
            return buildEmptyResult(request, availableWeapons, validEnemies);
        }

        // 3. 弹药充足性检查
        Map<String, Boolean> ammoSufficiency = checkAmmoSufficiency(availableWeapons, constraints);

        // 4. 火力匹配验证
        Map<String, List<EnemyNode>> candidates = matchWeaponEnemy(availableWeapons, validEnemies, fireTypeMap, zoneByWeaponId);

        // 5. 调度成本计算
        Map<String, Double> dispatchCosts = calculateDispatchCosts(candidates, availableWeapons, fireTypeMap, zoneByWeaponId);

        // 6. 敌我位置关系分析
        Map<String, Boolean> inZoneStatus = analyzeInZoneStatus(validEnemies, safeZones);

        return buildResult(
                availableWeapons,
                validEnemies,
                candidates,
                dispatchCosts,
                inZoneStatus,
                ammoSufficiency,
                request,
                constraints,
                fireTypeMap,
                weaponTypeMap,
                zoneByWeaponId);
    }

    private List<WeaponNode> buildVirtualWeapons(
            DynamicFormationRequestDTO request,
            List<ProtectionZone> zones,
            Map<String, WeaponType> weaponTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {
        List<String> selectedTypes = safeList(request.getSelectedWeaponTypes()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();

        if (selectedTypes.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProtectionZone> safeZones = safeList(zones);
        List<WeaponNode> result = new ArrayList<>();
        int sequence = 1;
        for (String weaponTypeName : selectedTypes) {
            WeaponType weaponType = weaponTypeMap.get(weaponTypeName);
            if (weaponType == null) {
                continue;
            }

            if (safeZones.isEmpty()) {
                WeaponNode node = buildVirtualWeaponNode(weaponType, "GLOBAL", sequence++);
                result.add(node);
            } else {
                for (ProtectionZone zone : safeZones) {
                    String zoneId = zone == null ? "UNKNOWN_ZONE" : String.valueOf(zone.getId());
                    WeaponNode node = buildVirtualWeaponNode(weaponType, zoneId, sequence++);
                    result.add(node);
                    zoneByWeaponId.put(node.getId(), zone);
                }
            }
        }
        return result;
    }

    private WeaponNode buildVirtualWeaponNode(WeaponType weaponType, String zoneId, int sequence) {
        WeaponNode node = new WeaponNode();
        String typeName = weaponType.getType() == null ? "UNKNOWN" : weaponType.getType();
        node.setId("V-" + sanitizeIdSegment(typeName) + "-" + sanitizeIdSegment(zoneId) + "-" + sequence);
        node.setType(typeName);
        node.setStatus(0);

        List<WeaponNode.NodeAmmoState> ammoStates = new ArrayList<>();
        for (WeaponType.FireTypeAllocation allocation : safeList(weaponType.getFireTypes())) {
            if (allocation == null || allocation.getFireType() == null) {
                continue;
            }
            WeaponNode.NodeAmmoState ammoState = new WeaponNode.NodeAmmoState();
            ammoState.setFireUnitType(allocation.getFireType());
            int quantity = allocation.getQuantity() == null ? 0 : Math.max(allocation.getQuantity(), 0);
            ammoState.setCurrentCount(quantity);
            ammoStates.add(ammoState);
        }
        node.setAmmoStates(ammoStates);
        return node;
    }

    private List<WeaponNode> filterAvailableWeapons(List<WeaponNode> weaponNodes) {
        return weaponNodes.stream()
                .filter(Objects::nonNull)
                .filter(w -> w.getStatus() == null || w.getStatus() == 0)
                .filter(this::hasAmmo)
                .toList();
    }

    /**
     * 过滤有效目标
     */
    private List<EnemyNode> filterValidEnemies(DynamicFormationRequestDTO request, List<EnemyNode> enemyNodes) {
        Set<String> selectedEnemyIds = new LinkedHashSet<>(safeList(request.getSelectedEnemyIds()));
        Set<String> knownEnemyTypes = safeList(request.getEnemyTypes()).stream()
                .filter(Objects::nonNull)
                .map(EnemyType::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return enemyNodes.stream()
                .filter(Objects::nonNull)
                .filter(e -> selectedEnemyIds.isEmpty() || selectedEnemyIds.contains(e.getId()))
                .filter(e -> knownEnemyTypes.isEmpty() || knownEnemyTypes.contains(e.getType()))
                .filter(e -> e.getAltitude() != null)
                .toList();
    }

    /**
     * 弹药充足性检查
     */
    private Map<String, Boolean> checkAmmoSufficiency(
            List<WeaponNode> weapons, DynamicFormationRequestDTO.FormationConstraints constraints) {
        Map<String, Boolean> result = new HashMap<>();
        boolean requireSufficiency = Boolean.TRUE.equals(constraints.getRequireAmmoSufficiency());
        double threshold = clamp(constraints.getAmmoThreshold() != null ? constraints.getAmmoThreshold() : 0.3, 0D, 1D);

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
     * 弹药充足性检查
     */
    private boolean isAmmoSufficient(WeaponNode weapon, double threshold) {
        if (weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return false;
        }
        List<WeaponNode.NodeAmmoState> ammoStates = weapon.getAmmoStates().stream()
                .filter(Objects::nonNull)
                .toList();
        int total = ammoStates.stream()
                .mapToInt(ammo -> ammo.getCurrentCount() != null ? ammo.getCurrentCount() : 0)
                .sum();
        long loadedTypeCount = ammoStates.stream()
                .filter(ammo -> ammo.getCurrentCount() != null && ammo.getCurrentCount() > 0)
                .count();
        double loadRatio = ammoStates.isEmpty() ? 0D : loadedTypeCount / (double) ammoStates.size();
        return total > 0 && loadRatio >= threshold;
    }

    /**
     * 火力匹配验证
     */
    private Map<String, List<EnemyNode>> matchWeaponEnemy(List<WeaponNode> weapons,
                                                          List<EnemyNode> enemies,
                                                          Map<String, FireType> fireTypeMap,
                                                          Map<String, ProtectionZone> zoneByWeaponId) {
        Map<String, List<EnemyNode>> candidates = new HashMap<>();
        for (WeaponNode weapon : weapons) {
            List<EnemyNode> matched = enemies.stream()
                    .filter(enemy -> isInRange(weapon, enemy, fireTypeMap, zoneByWeaponId))
                    .filter(enemy -> isInAltitude(weapon, enemy, fireTypeMap))
                    .toList();
            candidates.put(weapon.getId(), matched);
        }
        return candidates;
    }

    /**
     * 火力匹配验证
     */
    private boolean isInRange(WeaponNode weapon,
                              EnemyNode enemy,
                              Map<String, FireType> fireTypeMap,
                              Map<String, ProtectionZone> zoneByWeaponId) {
        if (weapon == null || enemy == null || weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return false;
        }
        ProtectionZone sourceZone = zoneByWeaponId.get(weapon.getId());
        Double distanceKm = computeDistanceKm(sourceZone, enemy);
        boolean hasRangeConfig = false;
        for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
            if (ammo == null || ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) {
                continue;
            }
            FireType fireType = fireTypeMap.get(ammo.getFireUnitType());
            if (fireType == null) {
                continue;
            }
            double maxRangeKm = defaultDouble(fireType.getMaxRange()) / 1000D;
            double minRangeKm = Math.max(defaultDouble(fireType.getMinRange()) / 1000D, 0D);
            if (maxRangeKm <= 0D) {
                continue;
            }
            hasRangeConfig = true;
            if (distanceKm == null || (distanceKm >= minRangeKm && distanceKm <= maxRangeKm)) {
                return true;
            }
        }
        return !hasRangeConfig;
    }

    /**
     * 火力匹配验证
     */
    private boolean isInAltitude(WeaponNode weapon, EnemyNode enemy, Map<String, FireType> fireTypeMap) {
        if (weapon == null || enemy == null || enemy.getAltitude() == null
                || weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return false;
        }
        double altitude = enemy.getAltitude();
        boolean hasAltitudeConfig = false;
        for (WeaponNode.NodeAmmoState ammo : weapon.getAmmoStates()) {
            if (ammo == null || ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) {
                continue;
            }
            FireType fireType = fireTypeMap.get(ammo.getFireUnitType());
            if (fireType == null) {
                continue;
            }
            if (fireType.getMinAlt() != null || fireType.getMaxAlt() != null) {
                hasAltitudeConfig = true;
            }
            double minAlt = Math.max(defaultDouble(fireType.getMinAlt()), 0D);
            double maxAlt = fireType.getMaxAlt() == null ? Double.MAX_VALUE : Math.max(defaultDouble(fireType.getMaxAlt()), minAlt);
            if (altitude >= minAlt && altitude <= maxAlt) {
                return true;
            }
        }
        return !hasAltitudeConfig;
    }

    /**
     * 调度成本计算
     */
    private Map<String, Double> calculateDispatchCosts(
            Map<String, List<EnemyNode>> candidates,
            List<WeaponNode> availableWeapons,
            Map<String, FireType> fireTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {
        Map<String, Double> costs = new HashMap<>();
        Map<String, WeaponNode> weaponById = availableWeapons.stream()
                .collect(Collectors.toMap(WeaponNode::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<String, List<EnemyNode>> entry : candidates.entrySet()) {
            String weaponId = entry.getKey();
            List<EnemyNode> targets = entry.getValue();
            if (!targets.isEmpty()) {
                WeaponNode weapon = weaponById.get(weaponId);
                ProtectionZone sourceZone = zoneByWeaponId.get(weaponId);
                double unitCost = resolveDispatchUnitCost(weapon, fireTypeMap);
                double totalCost = 0D;
                for (EnemyNode target : targets) {
                    Double distanceKm = computeDistanceKm(sourceZone, target);
                    totalCost += (distanceKm == null ? 0D : distanceKm) * unitCost;
                }
                costs.put(weaponId, round(totalCost));
            }
        }
        return costs;
    }

    /**
     * 敌我位置关系分析
     */
    private Map<String, Boolean> analyzeInZoneStatus(List<EnemyNode> enemies, List<ProtectionZone> zones) {
        List<ProtectionZone> safeZones = safeList(zones);
        Map<String, Boolean> inZoneStatus = new HashMap<>();
        for (EnemyNode enemy : enemies) {
            boolean inZone = safeZones.stream().anyMatch(zone -> isEnemyInZone(enemy, zone));
            inZoneStatus.put(enemy.getId(), inZone);
        }
        return inZoneStatus;
    }

    /**
     * 敌我位置关系分析
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
     * 构建结果
     */
    private DynamicFormationResultDTO buildResult(
            List<WeaponNode> availableWeapons,
            List<EnemyNode> validEnemies,
            Map<String, List<EnemyNode>> candidates,
            Map<String, Double> dispatchCosts,
            Map<String, Boolean> inZoneStatus,
            Map<String, Boolean> ammoSufficiency,
            DynamicFormationRequestDTO request,
            DynamicFormationRequestDTO.FormationConstraints constraints,
            Map<String, FireType> fireTypeMap,
            Map<String, WeaponType> weaponTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {

        DynamicFormationResultDTO result = new DynamicFormationResultDTO();
        result.setAlgorithmType(request.getConfig() != null ? request.getConfig().getAlgorithmType() : "DQN");
        result.setParadigm(request.getParadigm());
        result.setEnemyCount(validEnemies.size());
        result.setWeaponCount(availableWeapons.size());
        result.setTotalAmmo(availableWeapons.stream()
                .flatMap(weapon -> safeList(weapon.getAmmoStates()).stream())
                .mapToInt(ammo -> ammo.getCurrentCount() == null ? 0 : ammo.getCurrentCount())
                .sum());
        result.setTotalChannels(availableWeapons.stream()
                .mapToInt(weapon -> resolveChannelCount(weapon, weaponTypeMap))
                .sum());
        result.setEstimatedCost(round(dispatchCosts.values().stream().mapToDouble(this::defaultDouble).sum()));

        List<DynamicFormationResultDTO.DynamicFormationPlanDTO> plans = generateHeuristicPlans(
                availableWeapons,
                candidates,
                inZoneStatus,
                ammoSufficiency,
                constraints,
                request,
                fireTypeMap,
                weaponTypeMap,
                zoneByWeaponId);
        result.setPlans(plans);
        result.setRecommendedPlanId(plans.stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getFeasible()))
                .map(DynamicFormationResultDTO.DynamicFormationPlanDTO::getPlanId)
                .findFirst()
                .orElse(plans.isEmpty() ? null : plans.get(0).getPlanId()));

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
        result.setRecommendedPlanId(null);

        return result;
    }

    private List<DynamicFormationResultDTO.DynamicFormationPlanDTO> generateHeuristicPlans(
            List<WeaponNode> availableWeapons,
            Map<String, List<EnemyNode>> candidates,
            Map<String, Boolean> inZoneStatus,
            Map<String, Boolean> ammoSufficiency,
            DynamicFormationRequestDTO.FormationConstraints constraints,
            DynamicFormationRequestDTO request,
            Map<String, FireType> fireTypeMap,
            Map<String, WeaponType> weaponTypeMap,
            Map<String, ProtectionZone> zoneByWeaponId) {
        if (availableWeapons.isEmpty() || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        int maxGroupSize = constraints.getMaxGroupSize() == null || constraints.getMaxGroupSize() <= 0
                ? 6 : constraints.getMaxGroupSize();
        double minInterceptionRate = clamp(defaultDouble(constraints.getMinInterceptionRate()), 0D, 1D);
        Set<String> allowedDomains = resolveAllowedDomains(request.getParadigm());

        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (WeaponNode weapon : availableWeapons) {
            WeaponType weaponType = weaponTypeMap.get(weapon.getType());
            String deployDomain = normalizeDomain(weaponType == null ? null : weaponType.getDeployDomain());
            if (!allowedDomains.isEmpty() && !allowedDomains.contains(deployDomain)) {
                continue;
            }
            for (EnemyNode enemy : candidates.getOrDefault(weapon.getId(), List.of())) {
                FireType bestFireType = resolveBestFireType(weapon, enemy, fireTypeMap, zoneByWeaponId.get(weapon.getId()), minInterceptionRate);
                if (bestFireType == null) {
                    continue;
                }
                WeaponNode.NodeAmmoState ammoState = resolveAmmoState(weapon, bestFireType.getType());
                if (ammoState == null || ammoState.getCurrentCount() == null || ammoState.getCurrentCount() <= 0) {
                    continue;
                }
                Double distanceKm = computeDistanceKm(zoneByWeaponId.get(weapon.getId()), enemy);
                double normalizedDistance = distanceKm == null ? 0.55D
                        : clamp(1D - distanceKm / Math.max(defaultDouble(bestFireType.getMaxRange()) / 1000D, 1D), 0D, 1D);
                double interception = clamp(defaultDouble(bestFireType.getInterception()), 0D, 1D);
                boolean targetInZone = Boolean.TRUE.equals(inZoneStatus.get(enemy.getId()));
                double zoneBonus = Boolean.TRUE.equals(constraints.getPrioritizeInZoneEnemies()) && targetInZone ? 0.15D : 0D;
                double ammoFactor = clamp(ammoState.getCurrentCount() / 6D, 0D, 1D) * 0.12D;
                double dispatchCost = (distanceKm == null ? 0D : distanceKm) * Math.max(defaultDouble(bestFireType.getAttCost()), 0D);
                double dispatchPenalty = Boolean.TRUE.equals(constraints.getConsiderDispatchCost())
                        ? clamp(dispatchCost / 180D, 0D, 0.25D) * clamp(defaultDouble(constraints.getDispatchCostWeight()), 0D, 1D)
                        : 0D;
                double score = clamp(interception * 0.56D + normalizedDistance * 0.32D + zoneBonus + ammoFactor - dispatchPenalty, 0D, 1D);
                scoredCandidates.add(new ScoredCandidate(weapon, enemy, bestFireType, deployDomain, zoneByWeaponId.get(weapon.getId()), distanceKm, dispatchCost, targetInZone, score));
            }
        }
        scoredCandidates.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());
        if (scoredCandidates.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> selectedWeapons = new HashSet<>();
        Set<String> selectedEnemies = new HashSet<>();
        Map<String, Integer> remainingAmmo = buildRemainingAmmoMap(availableWeapons);
        List<DynamicFormationResultDTO.DynamicAllocationDetail> details = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double fitnessScoreTotal = 0D;

        for (ScoredCandidate candidate : scoredCandidates) {
            if (details.size() >= maxGroupSize) {
                break;
            }
            String weaponId = candidate.weapon().getId();
            String enemyId = candidate.enemy().getId();
            if (selectedWeapons.contains(weaponId) || selectedEnemies.contains(enemyId)) {
                continue;
            }
            String ammoKey = weaponId + "|" + candidate.fireType().getType();
            int ammoBefore = remainingAmmo.getOrDefault(ammoKey, 0);
            if (ammoBefore <= 0) {
                continue;
            }
            selectedWeapons.add(weaponId);
            selectedEnemies.add(enemyId);
            remainingAmmo.put(ammoKey, ammoBefore - 1);

            DynamicFormationResultDTO.DynamicAllocationDetail detail = new DynamicFormationResultDTO.DynamicAllocationDetail();
            detail.setWeaponNodeId(weaponId);
            detail.setWeaponType(candidate.weapon().getType());
            detail.setFireType(candidate.fireType().getType());
            detail.setDeployDomain(candidate.deployDomain());
            detail.setTargetEnemyId(enemyId);
            detail.setTargetEnemyType(candidate.enemy().getType());
            detail.setSourceZoneId(candidate.sourceZone() == null ? "UNASSIGNED_ZONE" : candidate.sourceZone().getId());
            detail.setTargetInZone(candidate.targetInZone());
            detail.setDistanceKm(round(candidate.distanceKm() == null ? 0D : candidate.distanceKm()));
            detail.setAssignmentScore(round(candidate.score() * 100D));
            detail.setInterceptionRate(round(clamp(defaultDouble(candidate.fireType().getInterception()), 0D, 1D) * 100D));
            detail.setAmmoSufficient(ammoSufficiency.getOrDefault(weaponId, true));
            detail.setAmmoBefore(ammoBefore);
            detail.setAmmoAfter(ammoBefore - 1);
            detail.setDispatchCost(round(candidate.dispatchCost()));
            details.add(detail);
            fitnessScoreTotal += candidate.score();
        }

        DynamicFormationResultDTO.DynamicFormationPlanDTO plan = new DynamicFormationResultDTO.DynamicFormationPlanDTO();
        plan.setPlanId(UUID.randomUUID().toString());
        plan.setPlanName((request.getConfig() == null ? "DQN" : request.getConfig().getAlgorithmType()) + "-HEURISTIC");
        plan.setFeasible(!details.isEmpty());
        plan.setFitnessScore(round(details.isEmpty() ? 0D : (fitnessScoreTotal / details.size()) * 100D));
        plan.setGroupSize(selectedWeapons.size());
        plan.setAllocatedEnemyCount(selectedEnemies.size());
        plan.setParticipatingDomains(details.stream()
                .map(DynamicFormationResultDTO.DynamicAllocationDetail::getDeployDomain)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        if (details.isEmpty()) {
            warnings.add("当前约束下未生成可执行动态分配方案。");
        }
        if (details.size() < Math.min(maxGroupSize, availableWeapons.size())) {
            warnings.add("部分武器或目标未满足约束，已生成局部最优分配。");
        }
        if (ammoSufficiency.containsValue(false)) {
            warnings.add("存在弹药储备偏低武器，已在方案中降权处理。");
        }
        plan.setWarnings(warnings);
        plan.setDetails(details);
        return new ArrayList<>(List.of(plan));
    }

    private Map<String, Integer> buildRemainingAmmoMap(List<WeaponNode> availableWeapons) {
        Map<String, Integer> remainingAmmo = new HashMap<>();
        for (WeaponNode weapon : availableWeapons) {
            for (WeaponNode.NodeAmmoState ammo : safeList(weapon.getAmmoStates())) {
                remainingAmmo.put(weapon.getId() + "|" + ammo.getFireUnitType(), ammo.getCurrentCount() == null ? 0 : ammo.getCurrentCount());
            }
        }
        return remainingAmmo;
    }

    private FireType resolveBestFireType(WeaponNode weapon,
                                         EnemyNode enemy,
                                         Map<String, FireType> fireTypeMap,
                                         ProtectionZone sourceZone,
                                         double minInterceptionRate) {
        FireType best = null;
        double bestScore = -1D;
        for (WeaponNode.NodeAmmoState ammo : safeList(weapon.getAmmoStates())) {
            if (ammo == null || ammo.getCurrentCount() == null || ammo.getCurrentCount() <= 0) {
                continue;
            }
            FireType fireType = fireTypeMap.get(ammo.getFireUnitType());
            if (fireType == null) {
                continue;
            }
            double interception = clamp(defaultDouble(fireType.getInterception()), 0D, 1D);
            if (interception < minInterceptionRate) {
                continue;
            }
            if (!isFireTypeInRange(fireType, sourceZone, enemy) || !isFireTypeInAltitude(fireType, enemy)) {
                continue;
            }
            Double distanceKm = computeDistanceKm(sourceZone, enemy);
            double rangeKm = Math.max(defaultDouble(fireType.getMaxRange()) / 1000D, 1D);
            double distanceFactor = distanceKm == null ? 0.55D : clamp(1D - distanceKm / rangeKm, 0D, 1D);
            double score = interception * 0.7D + distanceFactor * 0.3D;
            if (score > bestScore) {
                bestScore = score;
                best = fireType;
            }
        }
        return best;
    }

    private boolean isFireTypeInRange(FireType fireType, ProtectionZone zone, EnemyNode enemy) {
        double maxRangeKm = defaultDouble(fireType.getMaxRange()) / 1000D;
        if (maxRangeKm <= 0D) {
            return true;
        }
        double minRangeKm = Math.max(defaultDouble(fireType.getMinRange()) / 1000D, 0D);
        Double distanceKm = computeDistanceKm(zone, enemy);
        return distanceKm == null || (distanceKm >= minRangeKm && distanceKm <= maxRangeKm);
    }

    private boolean isFireTypeInAltitude(FireType fireType, EnemyNode enemy) {
        if (enemy.getAltitude() == null) {
            return false;
        }
        double altitude = enemy.getAltitude();
        double minAlt = Math.max(defaultDouble(fireType.getMinAlt()), 0D);
        double maxAlt = fireType.getMaxAlt() == null ? Double.MAX_VALUE : Math.max(defaultDouble(fireType.getMaxAlt()), minAlt);
        return altitude >= minAlt && altitude <= maxAlt;
    }

    private int resolveChannelCount(WeaponNode weapon, Map<String, WeaponType> weaponTypeMap) {
        WeaponType weaponType = weaponTypeMap.get(weapon.getType());
        if (weaponType == null || weaponType.getChannelCount() == null || weaponType.getChannelCount() <= 0) {
            return 1;
        }
        return weaponType.getChannelCount();
    }

    private double resolveDispatchUnitCost(WeaponNode weapon, Map<String, FireType> fireTypeMap) {
        if (weapon == null || weapon.getAmmoStates() == null || weapon.getAmmoStates().isEmpty()) {
            return 0D;
        }
        return weapon.getAmmoStates().stream()
                .filter(Objects::nonNull)
                .filter(ammo -> ammo.getCurrentCount() != null && ammo.getCurrentCount() > 0)
                .map(ammo -> fireTypeMap.get(ammo.getFireUnitType()))
                .filter(Objects::nonNull)
                .mapToDouble(fireType -> Math.max(defaultDouble(fireType.getAttCost()), 0D))
                .filter(cost -> cost > 0D)
                .min()
                .orElse(0D);
    }

    private Set<String> resolveAllowedDomains(FormationParadigm paradigm) {
        if (paradigm == null || paradigm == FormationParadigm.ALL) {
            return Set.of();
        }
        Set<String> domains = new LinkedHashSet<>();
        for (String value : paradigm.name().split("_")) {
            switch (value) {
                case "AIR", "GROUND", "SEA", "SPACE" -> domains.add(value);
                default -> {
                }
            }
        }
        return domains;
    }

    private String normalizeDomain(String rawDomain) {
        if (rawDomain == null) {
            return "UNKNOWN";
        }
        String normalized = rawDomain.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "UNKNOWN";
        }
        if (normalized.contains("空") || normalized.contains("air")) {
            return "AIR";
        }
        if (normalized.contains("地") || normalized.contains("陆") || normalized.contains("ground") || normalized.contains("land")) {
            return "GROUND";
        }
        if (normalized.contains("海") || normalized.contains("舰") || normalized.contains("sea") || normalized.contains("naval")) {
            return "SEA";
        }
        if (normalized.contains("天") || normalized.contains("太空") || normalized.contains("space")) {
            return "SPACE";
        }
        return rawDomain.toUpperCase(Locale.ROOT);
    }

    private WeaponNode.NodeAmmoState resolveAmmoState(WeaponNode weapon, String fireType) {
        for (WeaponNode.NodeAmmoState ammo : safeList(weapon.getAmmoStates())) {
            if (Objects.equals(ammo.getFireUnitType(), fireType)) {
                return ammo;
            }
        }
        return null;
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

    private String sanitizeIdSegment(String value) {
        if (value == null) {
            return "NA";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.isBlank()) {
            return "NA";
        }
        return sanitized;
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

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record ScoredCandidate(
            WeaponNode weapon,
            EnemyNode enemy,
            FireType fireType,
            String deployDomain,
            ProtectionZone sourceZone,
            Double distanceKm,
            double dispatchCost,
            boolean targetInZone,
            double score) {
    }
}
