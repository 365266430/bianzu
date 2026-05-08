package com.bianzu.bianzu_backend.processor;

import com.bianzu.bianzu_backend.model.CombatEngagement;
import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.SimulationContext;
import com.bianzu.bianzu_backend.service.EnemyTypeService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnRuntimeConfigService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnTrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Component
public class CombatProcessor implements SimulationProcessor {

    private static final double EARTH_RADIUS_KM = 6371D;

    private final Random random = new Random(20260508L);

    @Autowired
    private DqnTrainingService dqnTrainingService;

    @Autowired
    private DqnRuntimeConfigService dqnRuntimeConfigService;

    @Autowired
    private EnemyTypeService enemyTypeService;

    @Override
    public void process(SimulationContext context) {
        if (context == null) {
            return;
        }

        Set<String> destroyedEnemyIds = resolveEngagements(context);
        if (!destroyedEnemyIds.isEmpty()) {
            context.getEnemies().removeIf(enemy -> enemy != null && destroyedEnemyIds.contains(enemy.getId()));
        }
        applySurvivingEnemyDamage(context);
    }

    private Set<String> resolveEngagements(SimulationContext context) {
        List<CombatEngagement> engagements = safeList(context.getEngagements());
        if (engagements.isEmpty() || safeList(context.getEnemies()).isEmpty()) {
            dqnTrainingService.finishEpisode(0.001D, 32, 10);
            return Collections.emptySet();
        }

        Set<String> existingEnemyIds = new HashSet<>();
        for (EnemyNode enemy : safeList(context.getEnemies())) {
            if (enemy != null && enemy.getId() != null) {
                existingEnemyIds.add(enemy.getId());
            }
        }

        Set<String> destroyedEnemyIds = new HashSet<>();
        boolean trainingEnabled = dqnRuntimeConfigService.isTrainingEnabled();
        for (CombatEngagement engagement : engagements) {
            if (engagement == null || engagement.getEnemyId() == null || destroyedEnemyIds.contains(engagement.getEnemyId())) {
                continue;
            }
            boolean invalidAction = !existingEnemyIds.contains(engagement.getEnemyId());
            double killProbability = clamp(defaultDouble(engagement.getInterceptionRate()), 0D, 0.98D);
            boolean destroyed = !invalidAction && random.nextDouble() <= killProbability;
            if (destroyed) {
                destroyedEnemyIds.add(engagement.getEnemyId());
            }
            if (trainingEnabled) {
                dqnTrainingService.observeCombatOutcome(
                        engagement.getScoredAction(),
                        destroyed,
                        invalidAction,
                        0.001D,
                        32,
                        10);
            }
        }
        return destroyedEnemyIds;
    }

    private void applySurvivingEnemyDamage(SimulationContext context) {
        for (EnemyNode enemy : safeList(context.getEnemies())) {
            if (enemy == null) {
                continue;
            }
            EnemyType enemyType = enemy.getType() == null ? null : enemyTypeService.getEnemyType(enemy.getType());
            int damage = Math.max(1, (int) Math.round(defaultDouble(enemyType == null ? null : enemyType.getDamageCapability()) / 25D));
            for (ProtectionZone zone : safeList(context.getZones())) {
                if (isEnemyInZone(enemy, zone)) {
                    int currentHealth = zone.getHealth() == null ? 10 : zone.getHealth();
                    zone.setHealth(Math.max(0, currentHealth - damage));
                }
            }
        }
    }

    private boolean isEnemyInZone(EnemyNode enemy, ProtectionZone zone) {
        Double distanceKm = computeDistanceKm(zone, enemy);
        if (distanceKm == null) {
            return false;
        }
        double radiusKm = Math.max(defaultDouble(zone.getSize()) / 1000D, 0D);
        return radiusKm > 0D && distanceKm <= radiusKm;
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
}
