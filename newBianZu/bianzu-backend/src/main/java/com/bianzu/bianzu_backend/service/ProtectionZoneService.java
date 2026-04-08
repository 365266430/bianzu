package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSONArray;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProtectionZoneService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeaponTypeService weaponService;

    private static final String KEY_BLUE_ZONES = "sim:state:blue_zones";

    public void initProtectionZones() {
        WeaponType hq9Launcher = weaponService.getWeaponByType("HQ-9_Launcher");
        WeaponType ld2000Ciws = weaponService.getWeaponByType("LD-2000_CIWS");

        if (hq9Launcher == null || ld2000Ciws == null) {
            throw new RuntimeException("Weapon type data is missing. Please initialize weapons first.");
        }

        List<ProtectionZone> zones = new ArrayList<>();

        ProtectionZone zoneBj = new ProtectionZone();
        zoneBj.setId("Zone-Beijing");
        zoneBj.setLocation(List.of(116.40, 39.90));
        zoneBj.setSize(25000.0);
        zoneBj.setValue(3);
        zoneBj.setHealth(10);
        zoneBj.setStationedWeaponIds(new ArrayList<>());
        zones.add(zoneBj);

        ProtectionZone zoneSh = new ProtectionZone();
        zoneSh.setId("Zone-Shanghai");
        zoneSh.setLocation(List.of(121.47, 31.23));
        zoneSh.setSize(15000.0);
        zoneSh.setValue(2);
        zoneSh.setHealth(10);
        zoneSh.setStationedWeaponIds(new ArrayList<>());
        zones.add(zoneSh);

        redisTemplate.opsForValue().set(KEY_BLUE_ZONES, zones);
        System.out.println(">>> Protection zones initialized: " + zones.size());
    }

    public List<ProtectionZone> getAllZones() {
        Object obj = redisTemplate.opsForValue().get(KEY_BLUE_ZONES);
        if (obj == null) {
            return new ArrayList<>();
        }
        return ((JSONArray) obj).toJavaList(ProtectionZone.class);
    }

    public void saveAllZones(List<ProtectionZone> zones) {
        if (zones == null) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_BLUE_ZONES, zones);
    }

    public List<ProtectionZone> appendZone(ProtectionZone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Protection zone cannot be null");
        }
        if (zone.getLocation() == null || zone.getLocation().size() < 2) {
            throw new IllegalArgumentException("Protection zone location cannot be empty");
        }
        if (zone.getSize() == null || zone.getSize() <= 0) {
            zone.setSize(25000D);
        }
        if (zone.getValue() == null) {
            zone.setValue(2);
        }
        if (zone.getHealth() == null) {
            zone.setHealth(10);
        }
        if (zone.getStationedWeaponIds() == null) {
            zone.setStationedWeaponIds(new ArrayList<>());
        }
        if (zone.getId() == null || zone.getId().isBlank()) {
            zone.setId("Zone-" + UUID.randomUUID().toString().substring(0, 8));
        }

        List<ProtectionZone> zones = getAllZones();
        zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
        zones.add(zone);
        saveAllZones(zones);
        return zones;
    }

    public List<ProtectionZone> removeZoneById(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("Protection zone id cannot be empty");
        }

        List<ProtectionZone> zones = getAllZones();
        zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);
        boolean removed = zones.removeIf(zone -> zoneId.equals(zone.getId()));
        if (!removed) {
            throw new IllegalArgumentException("Protection zone not found: " + zoneId);
        }

        saveAllZones(zones);
        return zones;
    }

    public List<ProtectionZone> updateZone(String zoneId, ProtectionZone updatedZone) {
        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("Protection zone id cannot be empty");
        }
        if (updatedZone == null) {
            throw new IllegalArgumentException("Updated protection zone cannot be null");
        }
        if (updatedZone.getLocation() == null || updatedZone.getLocation().size() < 2) {
            throw new IllegalArgumentException("Protection zone location cannot be empty");
        }
        if (updatedZone.getSize() == null || updatedZone.getSize() <= 0) {
            updatedZone.setSize(25000D);
        }
        if (updatedZone.getValue() == null) {
            updatedZone.setValue(2);
        }
        if (updatedZone.getHealth() == null) {
            updatedZone.setHealth(10);
        }

        List<ProtectionZone> zones = getAllZones();
        zones = zones == null ? new ArrayList<>() : new ArrayList<>(zones);

        boolean updated = false;
        for (int i = 0; i < zones.size(); i++) {
            ProtectionZone existing = zones.get(i);
            if (!zoneId.equals(existing.getId())) {
                continue;
            }

            if (updatedZone.getStationedWeaponIds() == null) {
                updatedZone.setStationedWeaponIds(existing.getStationedWeaponIds());
            }
            if (updatedZone.getId() == null || updatedZone.getId().isBlank()) {
                updatedZone.setId(existing.getId());
            }

            zones.set(i, updatedZone);
            updated = true;
            break;
        }

        if (!updated) {
            throw new IllegalArgumentException("Protection zone not found: " + zoneId);
        }

        saveAllZones(zones);
        return zones;
    }

    public List<ProtectionZone> clearZones() {
        List<ProtectionZone> zones = new ArrayList<>();
        saveAllZones(zones);
        return zones;
    }

    public boolean assignWeaponsToZone(String zoneId, List<String> weaponIds) {
        List<ProtectionZone> allZones = getAllZones();
        if (allZones.isEmpty()) {
            System.err.println("No protection zone data found");
            return false;
        }

        boolean found = false;
        for (ProtectionZone zone : allZones) {
            if (zone.getId().equals(zoneId)) {
                zone.setStationedWeaponIds(new ArrayList<>(weaponIds));
                found = true;
                System.out.println(">>> Updated zone " + zoneId + " with weapons: " + weaponIds.size());
                break;
            }
        }

        if (!found) {
            System.err.println("Protection zone not found: " + zoneId);
            return false;
        }

        saveAllZones(allZones);
        return true;
    }
}
