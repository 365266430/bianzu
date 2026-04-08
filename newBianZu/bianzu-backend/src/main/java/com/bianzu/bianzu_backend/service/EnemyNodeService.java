package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSONArray;
import com.bianzu.bianzu_backend.model.EnemyNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EnemyNodeService {
    @Autowired
    private RedisTemplate redisTemplate;

    private static final String STATE_RED_ENEMIES = "sim:state:red_enemies";

    public List<EnemyNode> getAllEnemies() {
        try {
            Object obj = redisTemplate.opsForValue().get(STATE_RED_ENEMIES);
            if (obj == null) {
                return null;
            }
            return ((JSONArray) obj).toJavaList(EnemyNode.class);
        } catch (Exception e) {
            System.out.println("Failed to read enemy nodes: " + e.getMessage());
            return null;
        }
    }

    public void saveAllEnemies(List<EnemyNode> enemies) {
        if (enemies == null) {
            return;
        }
        redisTemplate.opsForValue().set(STATE_RED_ENEMIES, enemies);
    }

    public List<EnemyNode> appendEnemy(EnemyNode enemy) {
        if (enemy == null) {
            throw new IllegalArgumentException("Enemy target cannot be null");
        }
        if (enemy.getType() == null || enemy.getType().isBlank()) {
            throw new IllegalArgumentException("Enemy target type cannot be empty");
        }
        if (enemy.getLatitude() == null || enemy.getLongitude() == null) {
            throw new IllegalArgumentException("Enemy target location cannot be empty");
        }
        if (enemy.getAltitude() == null) {
            enemy.setAltitude(10000D);
        }
        if (enemy.getHeading() == null) {
            enemy.setHeading(90D);
        }
        if (enemy.getSpeed() == null) {
            enemy.setSpeed(0D);
        }
        if (enemy.getId() == null || enemy.getId().isBlank()) {
            enemy.setId("E-" + UUID.randomUUID().toString().substring(0, 8));
        }

        List<EnemyNode> enemies = getAllEnemies();
        enemies = enemies == null ? new ArrayList<>() : new ArrayList<>(enemies);
        enemies.add(enemy);
        saveAllEnemies(enemies);
        return enemies;
    }

    public List<EnemyNode> removeEnemyById(String enemyId) {
        if (enemyId == null || enemyId.isBlank()) {
            throw new IllegalArgumentException("Enemy id cannot be empty");
        }

        List<EnemyNode> enemies = getAllEnemies();
        enemies = enemies == null ? new ArrayList<>() : new ArrayList<>(enemies);
        boolean removed = enemies.removeIf(enemy -> enemyId.equals(enemy.getId()));
        if (!removed) {
            throw new IllegalArgumentException("Enemy not found: " + enemyId);
        }

        saveAllEnemies(enemies);
        return enemies;
    }

    public List<EnemyNode> updateEnemy(String enemyId, EnemyNode updatedEnemy) {
        if (enemyId == null || enemyId.isBlank()) {
            throw new IllegalArgumentException("Enemy id cannot be empty");
        }
        if (updatedEnemy == null) {
            throw new IllegalArgumentException("Updated enemy target cannot be null");
        }
        if (updatedEnemy.getType() == null || updatedEnemy.getType().isBlank()) {
            throw new IllegalArgumentException("Enemy target type cannot be empty");
        }
        if (updatedEnemy.getLatitude() == null || updatedEnemy.getLongitude() == null) {
            throw new IllegalArgumentException("Enemy target location cannot be empty");
        }

        List<EnemyNode> enemies = getAllEnemies();
        enemies = enemies == null ? new ArrayList<>() : new ArrayList<>(enemies);

        boolean updated = false;
        for (int i = 0; i < enemies.size(); i++) {
            EnemyNode existing = enemies.get(i);
            if (!enemyId.equals(existing.getId())) {
                continue;
            }

            if (updatedEnemy.getAltitude() == null) {
                updatedEnemy.setAltitude(existing.getAltitude());
            }
            if (updatedEnemy.getHeading() == null) {
                updatedEnemy.setHeading(existing.getHeading());
            }
            if (updatedEnemy.getSpeed() == null) {
                updatedEnemy.setSpeed(existing.getSpeed());
            }
            if (updatedEnemy.getId() == null || updatedEnemy.getId().isBlank()) {
                updatedEnemy.setId(existing.getId());
            }

            enemies.set(i, updatedEnemy);
            updated = true;
            break;
        }

        if (!updated) {
            throw new IllegalArgumentException("Enemy not found: " + enemyId);
        }

        saveAllEnemies(enemies);
        return enemies;
    }

    public List<EnemyNode> clearEnemies() {
        List<EnemyNode> enemies = new ArrayList<>();
        saveAllEnemies(enemies);
        return enemies;
    }
}
