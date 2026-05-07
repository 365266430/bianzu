package com.bianzu.bianzu_backend.algorithm.dqn;

import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnAction;
import com.bianzu.bianzu_backend.algorithm.dqn.model.DqnFeatureVector;
import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class DqnFeatureBuilder {

    public DqnFeatureVector buildCandidateFeature(
            DqnAction action,
            WeaponNode weapon,
            WeaponType weaponType,
            EnemyNode enemy,
            EnemyType enemyType,
            FireType fireType,
            ProtectionZone sourceZone,
            boolean targetInZone,
            Double distanceKm,
            double dispatchCost,
            int enemyCount,
            int availableWeaponCount,
            int totalAmmo,
            int zoneCount) {

        DqnFeatureVector vector = new DqnFeatureVector();

        vector.put("enemyCount", normalize(enemyCount, 20D));
        vector.put("availableWeaponCount", normalize(availableWeaponCount, 20D));
        vector.put("totalAmmo", normalize(totalAmmo, 120D));
        vector.put("zoneCount", normalize(zoneCount, 10D));

        vector.put("enemyAltitude", normalize(defaultDouble(enemy == null ? null : enemy.getAltitude()), 30000D));
        vector.put("enemySpeed", normalize(defaultDouble(enemy == null ? null : enemy.getSpeed()), 5D));
        double heading = defaultDouble(enemy == null ? null : enemy.getHeading());
        vector.put("enemyHeadingSin", Math.sin(Math.toRadians(heading)));
        vector.put("enemyHeadingCos", Math.cos(Math.toRadians(heading)));
        vector.put("enemyValue", normalize(defaultDouble(enemyType == null ? null : enemyType.getValue()), 100D));
        vector.put("enemyDamage", normalize(defaultDouble(enemyType == null ? null : enemyType.getDamageCapability()), 100D));
        vector.put("enemyAttackRange", normalize(defaultDouble(enemyType == null ? null : enemyType.getMaxAttackRange()), 1000000D));
        vector.put("enemyRcs", normalize(defaultDouble(enemyType == null ? null : enemyType.getRcs()), 20D));

        int ammoCount = resolveAmmoCount(weapon, action == null ? null : action.getFireType());
        int initialAmmo = resolveInitialAmmo(weaponType, action == null ? null : action.getFireType(), ammoCount);
        vector.put("weaponIdle", weapon != null && Objects.equals(weapon.getStatus(), 0) ? 1D : 0D);
        vector.put("ammoCount", normalize(ammoCount, 12D));
        vector.put("ammoRatio", initialAmmo <= 0 ? 0D : clamp(ammoCount / (double) initialAmmo, 0D, 1D));
        vector.put("channelCount", normalize(defaultDouble(weaponType == null ? null : weaponType.getChannelCount()), 8D));

        double maxRangeKm = defaultDouble(fireType == null ? null : fireType.getMaxRange()) / 1000D;
        double minRangeKm = defaultDouble(fireType == null ? null : fireType.getMinRange()) / 1000D;
        double distance = distanceKm == null ? 0D : distanceKm;
        vector.put("interception", clamp(defaultDouble(fireType == null ? null : fireType.getInterception()), 0D, 1D));
        vector.put("maxRange", normalize(defaultDouble(fireType == null ? null : fireType.getMaxRange()), 1000000D));
        vector.put("minRange", normalize(defaultDouble(fireType == null ? null : fireType.getMinRange()), 100000D));
        vector.put("maxAlt", normalize(defaultDouble(fireType == null ? null : fireType.getMaxAlt()), 40000D));
        vector.put("minAlt", normalize(defaultDouble(fireType == null ? null : fireType.getMinAlt()), 30000D));
        vector.put("fireCost", normalize(defaultDouble(fireType == null ? null : fireType.getCost()), 100D));
        vector.put("attCost", normalize(defaultDouble(fireType == null ? null : fireType.getAttCost()), 20D));

        vector.put("distanceKm", normalize(distance, 1000D));
        vector.put("rangeMatched", isRangeMatched(distanceKm, minRangeKm, maxRangeKm) ? 1D : 0D);
        vector.put("rangeProximity", maxRangeKm <= 0D ? 0.5D : clamp(1D - distance / Math.max(maxRangeKm, 1D), 0D, 1D));
        vector.put("altitudeMatched", isAltitudeMatched(enemy, fireType) ? 1D : 0D);
        vector.put("targetInZone", targetInZone ? 1D : 0D);
        vector.put("zoneValue", normalize(defaultDouble(sourceZone == null ? null : sourceZone.getValue()), 3D));
        vector.put("zoneHealth", normalize(defaultDouble(sourceZone == null ? null : sourceZone.getHealth()), 10D));
        vector.put("dispatchCost", normalize(dispatchCost, 1000D));
        vector.put("threatScore", computeThreatScore(enemyType, targetInZone));

        return vector;
    }

    public double computeThreatScore(EnemyType enemyType, boolean targetInZone) {
        double zoneBonus = targetInZone ? 0.35D : 0D;
        double damageBonus = normalize(defaultDouble(enemyType == null ? null : enemyType.getDamageCapability()), 100D) * 0.4D;
        double attackRange = defaultDouble(enemyType == null ? null : enemyType.getMaxAttackRange());
        double rangeBonus = attackRange > 50000D ? 0.1D : attackRange > 10000D ? 0.05D : 0D;
        double valueBonus = normalize(defaultDouble(enemyType == null ? null : enemyType.getValue()), 100D) * 0.15D;
        return clamp(zoneBonus + damageBonus + rangeBonus + valueBonus, 0D, 1D);
    }

    private boolean isRangeMatched(Double distanceKm, double minRangeKm, double maxRangeKm) {
        if (maxRangeKm <= 0D || distanceKm == null) {
            return true;
        }
        return distanceKm >= Math.max(minRangeKm, 0D) && distanceKm <= maxRangeKm;
    }

    private boolean isAltitudeMatched(EnemyNode enemy, FireType fireType) {
        if (enemy == null || enemy.getAltitude() == null || fireType == null) {
            return false;
        }
        double minAlt = Math.max(defaultDouble(fireType.getMinAlt()), 0D);
        double maxAlt = fireType.getMaxAlt() == null ? Double.MAX_VALUE : Math.max(defaultDouble(fireType.getMaxAlt()), minAlt);
        return enemy.getAltitude() >= minAlt && enemy.getAltitude() <= maxAlt;
    }

    private int resolveAmmoCount(WeaponNode weapon, String fireType) {
        if (weapon == null || weapon.getAmmoStates() == null || fireType == null) {
            return 0;
        }
        return weapon.getAmmoStates().stream()
                .filter(Objects::nonNull)
                .filter(ammo -> Objects.equals(ammo.getFireUnitType(), fireType))
                .map(WeaponNode.NodeAmmoState::getCurrentCount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    private int resolveInitialAmmo(WeaponType weaponType, String fireType, int fallback) {
        List<WeaponType.FireTypeAllocation> allocations = weaponType == null ? List.of() : weaponType.getFireTypes();
        if (allocations != null && fireType != null) {
            for (WeaponType.FireTypeAllocation allocation : allocations) {
                if (allocation != null && Objects.equals(allocation.getFireType(), fireType)) {
                    return allocation.getQuantity() == null ? fallback : Math.max(allocation.getQuantity(), fallback);
                }
            }
        }
        return Math.max(fallback, 1);
    }

    private double normalize(double value, double max) {
        if (max <= 0D) {
            return 0D;
        }
        return clamp(value / max, 0D, 1D);
    }

    private double defaultDouble(Number value) {
        return value == null ? 0D : value.doubleValue();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
