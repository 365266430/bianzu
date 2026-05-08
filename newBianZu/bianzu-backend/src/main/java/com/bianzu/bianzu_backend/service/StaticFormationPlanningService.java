package com.bianzu.bianzu_backend.service;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.FormationPlanDTO;
import com.bianzu.bianzu_backend.model.dto.StaticFormationResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * @Author dongjun
 * @Date 2026/4/8 15:08
 *静态编组规划服务
 */
@Service
public class StaticFormationPlanningService {

    private static final List<String> DEFAULT_PARADIGMS = List.of("AIR_AIR", "AIR_GROUND", "GROUND_GROUND");

    @Autowired
    private WeaponTypeService weaponTypeService;

    @Autowired
    private FireTypeService fireTypeService;

    @Autowired
    private EnemyTypeService enemyTypeService;

    public StaticFormationResultDTO buildGreedyReport(List<WeaponNode> weapons,
                                                      List<EnemyNode> enemies,
                                                      List<ProtectionZone> zones,
                                                      AlgorithmConfigDTO config) {
        return buildReport("greedyFormationStrategy", weapons, enemies, zones, config);
    }

    public StaticFormationResultDTO buildGeneticReport(List<WeaponNode> weapons,
                                                       List<EnemyNode> enemies,
                                                       List<ProtectionZone> zones,
                                                       AlgorithmConfigDTO config) {
        return buildReport("geneticFormationStrategy", weapons, enemies, zones, config);
    }

    public StaticFormationResultDTO buildPsoReport(List<WeaponNode> weapons,
                                                  List<EnemyNode> enemies,
                                                  List<ProtectionZone> zones,
                                                  AlgorithmConfigDTO config) {
        return buildReport("psoFormationStrategy", weapons, enemies, zones, config);
    }

    public StaticFormationResultDTO buildAntColonyReport(List<WeaponNode> weapons,
                                                         List<EnemyNode> enemies,
                                                         List<ProtectionZone> zones,
                                                         AlgorithmConfigDTO config) {
        return buildReport("antColonyFormationStrategy", weapons, enemies, zones, config);
    }

    public StaticFormationResultDTO buildReport(String algorithmType,
                                                List<WeaponNode> weapons,
                                                List<EnemyNode> enemies,
                                                List<ProtectionZone> zones,
                                                AlgorithmConfigDTO config) {
        Map<String, WeaponType> weaponTypes = weaponTypeService.getWeaponTypes().stream()
                .collect(Collectors.toMap(WeaponType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, FireType> fireTypes = fireTypeService.getFireTypes().stream()
                .collect(Collectors.toMap(FireType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, EnemyType> enemyTypes = enemyTypeService.getEnemyTypes().stream()
                .collect(Collectors.toMap(EnemyType::getType, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<ProtectionZone> safeZones = zones == null ? new ArrayList<>() : zones;
        Map<String, ProtectionZone> zoneByWeaponId = new HashMap<>();
        for (ProtectionZone zone : safeZones) {
            if (zone.getStationedWeaponIds() == null) {
                continue;
            }
            for (String weaponId : zone.getStationedWeaponIds()) {
                zoneByWeaponId.put(weaponId, zone);
            }
        }

        List<WeaponResource> resources = buildWeaponResources(weapons, weaponTypes, fireTypes, zoneByWeaponId, safeZones);
        List<String> paradigms = resolveParadigms(config);
        List<FormationPlanDTO> plans = paradigms.stream()
                .map(paradigm -> buildPlan(paradigm, algorithmType, config, resources, enemies, enemyTypes, safeZones))
                .sorted(Comparator.comparing(FormationPlanDTO::getFitnessScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        StaticFormationResultDTO result = new StaticFormationResultDTO();
        result.setAlgorithmType(algorithmType);
        result.setEnemyCount(enemies.size());
        result.setWeaponCount(resources.size());
        result.setZoneCount(safeZones.size());
        result.setTotalAmmo(resources.stream().mapToInt(resource -> resource.ammoCount).sum());
        result.setTotalChannels(resources.stream().mapToInt(resource -> resource.channelCount).sum());
        result.setSupportedParadigms(paradigms);
        result.setDomainResourceSummaries(buildDomainSummaries(resources));
        result.setFirepowerResourceSummaries(buildFirepowerSummaries(resources));
        result.setPlans(plans);
        result.setRecommendedPlanId(plans.stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getFeasible()))
                .findFirst()
                .map(FormationPlanDTO::getPlanId)
                .orElse(plans.isEmpty() ? null : plans.get(0).getPlanId()));
        return result;
    }

    private List<String> resolveParadigms(AlgorithmConfigDTO config) {
        if (config == null || config.getSelectedParadigms() == null || config.getSelectedParadigms().isEmpty()) {
            return DEFAULT_PARADIGMS;
        }
        return config.getSelectedParadigms().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private List<WeaponResource> buildWeaponResources(List<WeaponNode> weapons,
                                                      Map<String, WeaponType> weaponTypes,
                                                      Map<String, FireType> fireTypes,
                                                      Map<String, ProtectionZone> zoneByWeaponId,
                                                      List<ProtectionZone> zones) {
        ProtectionZone fallbackZone = zones.stream().findFirst().orElse(null);
        List<WeaponResource> resources = new ArrayList<>();
        for (WeaponNode weapon : weapons) {
            WeaponType weaponType = weaponTypes.get(weapon.getType());
            if (weaponType == null || weaponType.getFireTypes() == null || weaponType.getFireTypes().isEmpty()) {
                continue;
            }
            Map<String, Integer> ammoByType = new HashMap<>();
            if (weapon.getAmmoStates() != null) {
                for (WeaponNode.NodeAmmoState ammoState : weapon.getAmmoStates()) {
                    ammoByType.put(ammoState.getFireUnitType(), ammoState.getCurrentCount());
                }
            }
            for (WeaponType.FireTypeAllocation allocation : weaponType.getFireTypes()) {
                FireType fireType = fireTypes.get(allocation.getFireType());
                if (fireType == null) {
                    continue;
                }
                WeaponResource resource = new WeaponResource();
                resource.node = weapon;
                resource.weaponType = weaponType;
                resource.fireType = fireType;
                resource.zone = zoneByWeaponId.getOrDefault(weapon.getId(), fallbackZone);
                resource.deployDomain = normalizeDomain(weaponType.getDeployDomain());
                resource.ammoCount = ammoByType.getOrDefault(allocation.getFireType(), defaultInt(allocation.getQuantity()));
                resource.channelCount = Math.max(defaultInt(weaponType.getChannelCount()), 1);
                resource.interceptionRate = clamp(defaultDouble(fireType.getInterception()), 0D, 1D);
                resources.add(resource);
            }
        }
        return resources;
    }

    private List<StaticFormationResultDTO.DomainResourceSummary> buildDomainSummaries(List<WeaponResource> resources) {
        Map<String, List<WeaponResource>> grouped = resources.stream()
                .collect(Collectors.groupingBy(resource -> resource.deployDomain, LinkedHashMap::new, Collectors.toList()));
        List<StaticFormationResultDTO.DomainResourceSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<WeaponResource>> entry : grouped.entrySet()) {
            StaticFormationResultDTO.DomainResourceSummary summary = new StaticFormationResultDTO.DomainResourceSummary();
            summary.setDeployDomain(entry.getKey());
            summary.setWeaponNodeCount((int) entry.getValue().stream().map(resource -> resource.node.getId()).distinct().count());
            summary.setAmmoCount(entry.getValue().stream().mapToInt(resource -> resource.ammoCount).sum());
            summary.setChannelCount(entry.getValue().stream().mapToInt(resource -> resource.channelCount).sum());
            summaries.add(summary);
        }
        return summaries;
    }

    private List<StaticFormationResultDTO.FirepowerResourceSummary> buildFirepowerSummaries(List<WeaponResource> resources) {
        Map<String, List<WeaponResource>> grouped = resources.stream()
                .collect(Collectors.groupingBy(resource -> resource.deployDomain + "|" + resource.weaponType.getType() + "|" + resource.fireType.getType(), LinkedHashMap::new, Collectors.toList()));
        List<StaticFormationResultDTO.FirepowerResourceSummary> summaries = new ArrayList<>();
        for (List<WeaponResource> group : grouped.values()) {
            WeaponResource first = group.get(0);
            StaticFormationResultDTO.FirepowerResourceSummary summary = new StaticFormationResultDTO.FirepowerResourceSummary();
            summary.setDeployDomain(first.deployDomain);
            summary.setWeaponType(first.weaponType.getType());
            summary.setFireType(first.fireType.getType());
            summary.setWeaponNodeCount((int) group.stream().map(resource -> resource.node.getId()).distinct().count());
            summary.setAmmoCount(group.stream().mapToInt(resource -> resource.ammoCount).sum());
            summary.setChannelCount(group.stream().mapToInt(resource -> resource.channelCount).sum());
            summary.setAverageInterceptionRate(round(group.stream().mapToDouble(resource -> resource.interceptionRate).average().orElse(0D) * 100D));
            summary.setMaxRange(round(group.stream().mapToDouble(resource -> defaultDouble(resource.fireType.getMaxRange()) / 1000D).max().orElse(0D)));
            summaries.add(summary);
        }
        return summaries;
    }

    private FormationPlanDTO buildPlan(String paradigm,
                                       String algorithmType,
                                       AlgorithmConfigDTO config,
                                       List<WeaponResource> resources,
                                       List<EnemyNode> enemies,
                                       Map<String, EnemyType> enemyTypes,
                                       List<ProtectionZone> zones) {
        List<String> requiredDomains = switch (paradigm) {
            case "AIR_GROUND" -> List.of("AIR", "GROUND");
            case "GROUND_GROUND" -> List.of("GROUND", "GROUND");
            default -> List.of("AIR", "AIR");
        };
        Set<String> allowedDomains = new LinkedHashSet<>(requiredDomains);
        Set<String> allowedTypes = config == null || config.getAllowedWeaponTypes() == null ? Set.of() : new LinkedHashSet<>(config.getAllowedWeaponTypes());
        int maxGroupSize = config == null || config.getMaxGroupSize() == null || config.getMaxGroupSize() <= 0 ? 6 : config.getMaxGroupSize();

        List<WeaponResource> eligible = resources.stream()
                .filter(resource -> allowedDomains.contains(resource.deployDomain))
                .filter(resource -> resource.ammoCount > 0)
                .filter(resource -> allowedTypes.isEmpty() || allowedTypes.contains(resource.weaponType.getType()))
                .sorted(Comparator.comparing((WeaponResource resource) -> resource.interceptionRate).reversed().thenComparing(resource -> resource.ammoCount, Comparator.reverseOrder()))
                .limit(maxGroupSize)
                .toList();

        FormationPlanDTO plan = new FormationPlanDTO();
        plan.setPlanId(UUID.randomUUID().toString());
        plan.setPlanName(resolveAlgorithmName(algorithmType) + "-" + paradigm);
        plan.setParadigm(paradigm);
        plan.setParticipatingDomains(eligible.stream().map(resource -> resource.deployDomain).distinct().toList());
        plan.setGroupSize((int) eligible.stream().map(resource -> resource.node.getId()).distinct().count());

        boolean hasEligibleResources = !eligible.isEmpty();
        boolean domainCoverageSatisfied = coversRequiredDomains(requiredDomains, eligible);
        List<String> warnings = new ArrayList<>();
        if (!domainCoverageSatisfied) {
            warnings.add("Insufficient domain coverage in current static posture.");
        }
        if (!hasEligibleResources) {
            warnings.add("No eligible weapon resources for this paradigm.");
        }

        List<EnemyPriorityProfile> prioritizedEnemies = enemies.stream()
                .map(enemy -> buildEnemyPriorityProfile(enemy, enemyTypes.get(enemy.getType()), zones))
                .sorted(Comparator.comparingDouble((EnemyPriorityProfile profile) -> profile.totalPriority).reversed())
                .toList();

        Map<String, Integer> remainingAmmo = eligible.stream().collect(Collectors.toMap(resource -> resource.node.getId() + "|" + resource.fireType.getType(), resource -> resource.ammoCount));
        Map<String, Integer> remainingChannels = eligible.stream().collect(Collectors.toMap(resource -> resource.node.getId(), resource -> resource.channelCount, Math::max));

        double distanceScoreSum = 0D;
        double firepowerScoreSum = 0D;
        double defenseScoreSum = 0D;
        double interceptionSum = 0D;
        double estimatedCost = 0D;

        List<ScoredAssignment> assignments = new ArrayList<>();
        allocateAssignments(assignments, prioritizedEnemies.stream().filter(profile -> profile.baselineShots > 0).toList(), eligible, remainingAmmo, remainingChannels, algorithmType, config);
        allocateAssignments(assignments, prioritizedEnemies.stream().filter(profile -> profile.additionalShots > 0).toList(), eligible, remainingAmmo, remainingChannels, algorithmType, config);
        allocateAssignments(assignments, prioritizedEnemies.stream().filter(profile -> profile.deferredShots > 0).toList(), eligible, remainingAmmo, remainingChannels, algorithmType, config);

        for (ScoredAssignment assignment : assignments) {
            FormationPlanDTO.AllocationDetail detail = new FormationPlanDTO.AllocationDetail();
            detail.setWeaponNodeId(assignment.resource.node.getId());
            detail.setWeaponType(assignment.resource.weaponType.getType());
            detail.setFireType(assignment.resource.fireType.getType());
            detail.setTargetEnemyId(assignment.priorityProfile.enemy.getId());
            detail.setTargetEnemyType(assignment.priorityProfile.enemy.getType());
            detail.setSourceZoneId(assignment.resource.zone == null ? "UNASSIGNED_ZONE" : assignment.resource.zone.getId());
            detail.setDeployDomain(assignment.resource.deployDomain);
            detail.setDistanceKm(round(assignment.distanceKm));
            detail.setAssignmentScore(round(assignment.totalScore * 100D));
            detail.setEstimatedInterceptionRate(round(assignment.resource.interceptionRate * 100D));
            plan.getDetails().add(detail);

            distanceScoreSum += assignment.distanceFactor;
            firepowerScoreSum += assignment.firepowerFactor;
            defenseScoreSum += assignment.defenseFactor;
            interceptionSum += assignment.resource.interceptionRate;
            estimatedCost += defaultDouble(assignment.resource.fireType.getCost());
        }

        int allocationCount = assignments.size();
        int allocatedEnemyCount = (int) assignments.stream()
                .map(assignment -> assignment.priorityProfile.enemy.getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
        double coverage = enemies.isEmpty() ? 0D : allocatedEnemyCount / (double) enemies.size();
        plan.setAllocatedEnemyCount(allocatedEnemyCount);
        plan.setWarnings(warnings);
        boolean feasible = hasEligibleResources && domainCoverageSatisfied && allocatedEnemyCount > 0;
        plan.setFeasible(feasible);
        plan.setDistanceScore(round(avgPercent(distanceScoreSum, allocationCount)));
        plan.setFirepowerScore(round(avgPercent(firepowerScoreSum, allocationCount)));
        plan.setDefenseScore(round(avgPercent(defenseScoreSum, allocationCount)));
        plan.setCoverageScore(round(coverage * 100D));
        plan.setExpectedInterceptionRate(round(avgPercent(interceptionSum, allocationCount)));
        plan.setEstimatedCost(round(estimatedCost));
        plan.setFitnessScore(round(computeFitnessScore(plan, algorithmType, config, hasEligibleResources, domainCoverageSatisfied)));
        plan.setSummary(resolvePlanSummary(plan, algorithmType, paradigm, allocationCount, hasEligibleResources, domainCoverageSatisfied));
        return plan;
    }

    private void allocateAssignments(List<ScoredAssignment> assignments,
                                     List<EnemyPriorityProfile> profiles,
                                     List<WeaponResource> eligible,
                                     Map<String, Integer> remainingAmmo,
                                     Map<String, Integer> remainingChannels,
                                     String algorithmType,
                                     AlgorithmConfigDTO config) {
        for (EnemyPriorityProfile profile : profiles) {
            int shotsToAllocate = resolveShotsForPass(profile);
            for (int i = 0; i < shotsToAllocate; i++) {
                ScoredAssignment best = chooseBestAssignment(profile, eligible, remainingAmmo, remainingChannels, algorithmType, config);
                if (best == null) {
                    break;
                }
                remainingAmmo.computeIfPresent(best.ammoKey, (key, value) -> Math.max(0, value - 1));
                remainingChannels.computeIfPresent(best.resource.node.getId(), (key, value) -> Math.max(0, value - 1));
                assignments.add(best);
            }
        }
    }

    private int resolveShotsForPass(EnemyPriorityProfile profile) {
        if (profile.baselineShots > 0) {
            int value = profile.baselineShots;
            profile.baselineShots = 0;
            return value;
        }
        if (profile.additionalShots > 0) {
            int value = profile.additionalShots;
            profile.additionalShots = 0;
            return value;
        }
        if (profile.deferredShots > 0) {
            int value = profile.deferredShots;
            profile.deferredShots = 0;
            return value;
        }
        return 0;
    }

    private ScoredAssignment chooseBestAssignment(EnemyPriorityProfile profile,
                                                  List<WeaponResource> eligible,
                                                  Map<String, Integer> remainingAmmo,
                                                  Map<String, Integer> remainingChannels,
                                                  String algorithmType,
                                                  AlgorithmConfigDTO config) {
        ScoredAssignment best = null;
        for (WeaponResource resource : eligible) {
            String ammoKey = resource.node.getId() + "|" + resource.fireType.getType();
            if (remainingAmmo.getOrDefault(ammoKey, 0) <= 0 || remainingChannels.getOrDefault(resource.node.getId(), 0) <= 0) {
                continue;
            }
            ScoredAssignment current = scoreAssignment(resource, profile, algorithmType, config);
            if (best == null || current.totalScore > best.totalScore) {
                best = current;
            }
        }
        return best;
    }

    private boolean coversRequiredDomains(List<String> requiredDomains, List<WeaponResource> resources) {
        Map<String, Long> counts = resources.stream().collect(Collectors.groupingBy(resource -> resource.deployDomain, Collectors.counting()));
        for (String domain : requiredDomains) {
            long current = counts.getOrDefault(domain, 0L);
            if (current <= 0) {
                return false;
            }
            counts.put(domain, current - 1);
        }
        return true;
    }

    private ScoredAssignment scoreAssignment(WeaponResource resource,
                                             EnemyPriorityProfile profile,
                                             String algorithmType,
                                             AlgorithmConfigDTO config) {
        double distanceWeight = config == null || config.getDistanceWeight() == null ? 0.34D : config.getDistanceWeight();
        double firepowerWeight = config == null || config.getFirepowerWeight() == null ? 0.38D : config.getFirepowerWeight();
        double defenseWeight = config == null || config.getDefenseWeight() == null ? 0.28D : config.getDefenseWeight();
        double totalWeight = Math.max(distanceWeight + firepowerWeight + defenseWeight, 0.01D);

        double distanceKm = computeDistanceKm(resource.zone, profile.enemy);
        double rangeKm = Math.max(defaultDouble(resource.fireType.getMaxRange()) / 1000D, 1D);
        double distanceFactor = resource.zone == null ? 0.55D : clamp(1D - distanceKm / rangeKm, 0D, 1D);
        double firepowerFactor = clamp(resource.interceptionRate * 0.7D + clamp(resource.ammoCount / 6D, 0D, 1D) * 0.3D, 0D, 1D);
        double zoneMatchFactor = resource.zone != null && Objects.equals(resource.zone.getId(), profile.priorityZoneId) ? 1D : 0D;
        double protectionFactor = clamp((resource.zone == null ? 0.35D : defaultDouble(resource.zone.getValue()) / 3D) * 0.35D
                + clamp(profile.zonePriority / 55D, 0D, 1D) * 0.4D
                + zoneMatchFactor * 0.25D, 0D, 1D);
        double defenseFactor = clamp(protectionFactor * 0.55D + clamp(profile.baseThreat / 100D, 0D, 1D) * 0.45D, 0D, 1D);
        double baseScore = ((distanceWeight * distanceFactor) + (firepowerWeight * firepowerFactor) + (defenseWeight * defenseFactor)) / totalWeight;
        double algorithmBias = switch (algorithmType) {
            case "greedyFormationStrategy" -> firepowerFactor * 0.16D + clamp(profile.totalPriority / 100D, 0D, 1D) * 0.08D;
            case "geneticFormationStrategy" -> computeBalanceFactor(distanceFactor, firepowerFactor, defenseFactor) * 0.12D + defenseFactor * 0.08D + clamp(profile.zonePriority / 55D, 0D, 1D) * 0.04D;
            case "antColonyFormationStrategy" -> distanceFactor * 0.18D + clamp(1D - distanceKm / 300D, 0D, 1D) * 0.06D + clamp(profile.zonePriority / 55D, 0D, 1D) * 0.05D;
            default -> ((distanceFactor + firepowerFactor + defenseFactor) / 3D) * 0.08D
                    + computeBalanceFactor(distanceFactor, firepowerFactor, defenseFactor) * 0.03D
                    + clamp(profile.totalPriority / 100D, 0D, 1D) * 0.06D;
        };

        ScoredAssignment assignment = new ScoredAssignment();
        assignment.resource = resource;
        assignment.priorityProfile = profile;
        assignment.ammoKey = resource.node.getId() + "|" + resource.fireType.getType();
        assignment.distanceKm = distanceKm;
        assignment.distanceFactor = distanceFactor;
        assignment.firepowerFactor = firepowerFactor;
        assignment.defenseFactor = defenseFactor;
        assignment.totalScore = clamp(baseScore + algorithmBias, 0D, 1.35D);
        return assignment;
    }

    private double computeFitnessScore(FormationPlanDTO plan,
                                       String algorithmType,
                                       AlgorithmConfigDTO config,
                                       boolean hasEligibleResources,
                                       boolean domainCoverageSatisfied) {
        double distanceWeight = config == null || config.getDistanceWeight() == null ? 0.34D : config.getDistanceWeight();
        double firepowerWeight = config == null || config.getFirepowerWeight() == null ? 0.38D : config.getFirepowerWeight();
        double defenseWeight = config == null || config.getDefenseWeight() == null ? 0.28D : config.getDefenseWeight();
        double totalWeight = Math.max(distanceWeight + firepowerWeight + defenseWeight, 0.01D);

        double weightedOperationalScore = (plan.getDistanceScore() * distanceWeight
                + plan.getFirepowerScore() * firepowerWeight
                + plan.getDefenseScore() * defenseWeight) / totalWeight;
        double algorithmProfileScore = computeAlgorithmProfileScore(algorithmType, plan);
        double rawScore = weightedOperationalScore * 0.72D + algorithmProfileScore * 0.18D + plan.getCoverageScore() * 0.10D;

        if (!hasEligibleResources) {
            rawScore *= 0.08D;
        } else if (!domainCoverageSatisfied) {
            rawScore *= 0.35D;
        } else if (!Boolean.TRUE.equals(plan.getFeasible())) {
            rawScore *= 0.22D;
        }

        return clamp(rawScore, 0D, 100D);
    }

    private double computeAlgorithmProfileScore(String algorithmType, FormationPlanDTO plan) {
        double balanceScore = computeBalanceScore(plan.getDistanceScore(), plan.getFirepowerScore(), plan.getDefenseScore());
        return clamp(switch (algorithmType) {
            case "greedyFormationStrategy" -> plan.getFirepowerScore() * 0.78D + plan.getCoverageScore() * 0.22D;
            case "geneticFormationStrategy" -> balanceScore * 0.5D + plan.getDefenseScore() * 0.3D + plan.getCoverageScore() * 0.2D;
            case "antColonyFormationStrategy" -> plan.getDistanceScore() * 0.7D + plan.getCoverageScore() * 0.3D;
            default -> ((plan.getDistanceScore() + plan.getFirepowerScore() + plan.getDefenseScore()) / 3D) * 0.75D + plan.getCoverageScore() * 0.25D;
        }, 0D, 100D);
    }

    private String resolvePlanSummary(FormationPlanDTO plan,
                                      String algorithmType,
                                      String paradigm,
                                      int allocationCount,
                                      boolean hasEligibleResources,
                                      boolean domainCoverageSatisfied) {
        if (!hasEligibleResources) {
            return "No eligible weapon resources were found for " + paradigm + ".";
        }
        if (!domainCoverageSatisfied) {
            return "Only partial assignments were produced; current static posture cannot satisfy the required domains for " + paradigm + ".";
        }
        if (!Boolean.TRUE.equals(plan.getFeasible())) {
            return "No executable assignment was found for " + paradigm + ".";
        }
        return resolveAlgorithmName(algorithmType) + " produced " + allocationCount + " static assignments for " + paradigm + ".";
    }

    private double computeBalanceScore(double first, double second, double third) {
        return clamp(100D - (Math.abs(first - second) + Math.abs(second - third) + Math.abs(first - third)) / 3D, 0D, 100D);
    }

    private double computeBalanceFactor(double first, double second, double third) {
        return computeBalanceScore(first * 100D, second * 100D, third * 100D) / 100D;
    }

    private EnemyPriorityProfile buildEnemyPriorityProfile(EnemyNode enemy,
                                                           EnemyType enemyType,
                                                           List<ProtectionZone> zones) {
        EnemyPriorityProfile profile = new EnemyPriorityProfile();
        profile.enemy = enemy;
        profile.baseThreat = threatScore(enemy, enemyType);

        ZonePriorityProfile zonePriority = resolveZonePriority(enemy, enemyType, zones);
        profile.priorityZoneId = zonePriority.zoneId;
        profile.zonePriority = zonePriority.score;
        profile.totalPriority = profile.baseThreat + profile.zonePriority;

        if (profile.totalPriority >= 80D || profile.zonePriority >= 30D || profile.baseThreat >= 55D) {
            profile.baselineShots = 1;
            profile.additionalShots = 1;
            profile.deferredShots = 0;
        } else if (profile.totalPriority >= 42D || profile.zonePriority >= 15D || profile.baseThreat >= 28D) {
            profile.baselineShots = 1;
            profile.additionalShots = 0;
            profile.deferredShots = 0;
        } else {
            profile.baselineShots = 0;
            profile.additionalShots = 0;
            profile.deferredShots = 1;
        }
        return profile;
    }

    private double threatScore(EnemyNode enemy, EnemyType enemyType) {
        return defaultDouble(enemyType == null ? null : enemyType.getValue()) * 0.45D
                + defaultDouble(enemyType == null ? null : enemyType.getDamageCapability()) * 0.35D
                + defaultDouble(enemyType == null ? null : enemyType.getMaxAttackRange()) / 10000D * 0.1D
                + defaultDouble(enemy.getSpeed()) * 0.02D;
    }

    private ZonePriorityProfile resolveZonePriority(EnemyNode enemy,
                                                    EnemyType enemyType,
                                                    List<ProtectionZone> zones) {
        ZonePriorityProfile best = new ZonePriorityProfile();
        for (ProtectionZone zone : zones) {
            if (zone == null || zone.getId() == null) {
                continue;
            }
            double distanceKm = computeDistanceKm(zone, enemy);
            double zoneRadiusKm = Math.max(defaultDouble(zone.getSize()) / 1000D, 1D);
            double edgeDistanceKm = Math.max(distanceKm - zoneRadiusKm, 0D);
            double attackRangeKm = Math.max(defaultDouble(enemyType == null ? null : enemyType.getMaxAttackRange()) / 1000D, 1D);
            double zoneValueFactor = clamp(defaultDouble(zone.getValue()) / 3D, 0D, 1D);
            double attackWindowFactor = clamp(1D - edgeDistanceKm / attackRangeKm, 0D, 1D);
            double proximityFactor = clamp(1D - edgeDistanceKm / 220D, 0D, 1D);
            double headingFactor = computeHeadingFactor(enemy, zone);
            double score = zoneValueFactor * 22D + attackWindowFactor * 18D + proximityFactor * 10D + headingFactor * 6D;
            if (score > best.score) {
                best.zoneId = zone.getId();
                best.score = score;
            }
        }
        return best;
    }

    private double computeHeadingFactor(EnemyNode enemy, ProtectionZone zone) {
        if (enemy == null || zone == null || enemy.getHeading() == null || enemy.getLongitude() == null || enemy.getLatitude() == null
                || zone.getLocation() == null || zone.getLocation().size() < 2) {
            return 0D;
        }
        double bearing = computeBearing(enemy.getLongitude(), enemy.getLatitude(), zone.getLocation().get(0), zone.getLocation().get(1));
        double diff = Math.abs(enemy.getHeading() - bearing) % 360D;
        double normalizedDiff = diff > 180D ? 360D - diff : diff;
        return clamp(1D - normalizedDiff / 180D, 0D, 1D);
    }

    private double computeBearing(double lon1, double lat1, double lon2, double lat2) {
        double startLat = Math.toRadians(lat1);
        double endLat = Math.toRadians(lat2);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(deltaLon) * Math.cos(endLat);
        double x = Math.cos(startLat) * Math.sin(endLat)
                - Math.sin(startLat) * Math.cos(endLat) * Math.cos(deltaLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360D) % 360D;
    }

    private String resolveAlgorithmName(String algorithmType) {
        return switch (algorithmType) {
            case "greedyFormationStrategy" -> "GREEDY";
            case "geneticFormationStrategy" -> "GENETIC";
            case "antColonyFormationStrategy" -> "ANT_COLONY";
            default -> "PSO";
        };
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
        if (normalized.contains("天") || normalized.contains("太空") || normalized.contains("空天") || normalized.contains("space")) {
            return "SPACE";
        }
        return rawDomain.toUpperCase(Locale.ROOT);
    }

    private double computeDistanceKm(ProtectionZone zone, EnemyNode enemy) {
        if (zone == null || zone.getLocation() == null || zone.getLocation().size() < 2 || enemy.getLatitude() == null || enemy.getLongitude() == null) {
            return 0D;
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
        return 6371D * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }

    private double avgPercent(double total, int count) {
        return count <= 0 ? 0D : total / count * 100D;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private double defaultDouble(Number value) {
        return value == null ? 0D : value.doubleValue();
    }

    private int defaultInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private static class WeaponResource {
        private WeaponNode node;
        private WeaponType weaponType;
        private ProtectionZone zone;
        private FireType fireType;
        private String deployDomain;
        private int ammoCount;
        private int channelCount;
        private double interceptionRate;
    }

    private static class ScoredAssignment {
        private WeaponResource resource;
        private EnemyPriorityProfile priorityProfile;
        private String ammoKey;
        private double distanceKm;
        private double distanceFactor;
        private double firepowerFactor;
        private double defenseFactor;
        private double totalScore;
    }

    private static class EnemyPriorityProfile {
        private EnemyNode enemy;
        private String priorityZoneId;
        private double baseThreat;
        private double zonePriority;
        private double totalPriority;
        private int baselineShots;
        private int additionalShots;
        private int deferredShots;
    }

    private static class ZonePriorityProfile {
        private String zoneId;
        private double score;
    }
}
