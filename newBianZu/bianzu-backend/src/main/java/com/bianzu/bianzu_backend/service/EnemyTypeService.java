package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.repository.EnemyTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EnemyTypeService {

    private static final String KEY_ENEMY_TYPES = "sim:config:enemy_types";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EnemyTypeRepository enemyTypeRepository;

    public EnemyType getEnemyType(String typeName) {
        Object obj = redisTemplate.opsForHash().get(KEY_ENEMY_TYPES, typeName);
        if (obj != null) {
            return toEnemyType(obj);
        }

        EnemyType enemyType = enemyTypeRepository.findById(typeName).orElse(null);
        if (enemyType != null) {
            redisTemplate.opsForHash().put(KEY_ENEMY_TYPES, enemyType.getType(), enemyType);
        }
        return enemyType;
    }

    public List<EnemyType> getEnemyTypes() {
        List<Object> cached = redisTemplate.opsForHash().values(KEY_ENEMY_TYPES);
        if (cached == null || cached.isEmpty()) {
            List<EnemyType> dbList = enemyTypeRepository.findAll();
            dbList.forEach(enemyType -> redisTemplate.opsForHash().put(KEY_ENEMY_TYPES, enemyType.getType(), enemyType));
            return dbList;
        }

        List<EnemyType> result = new ArrayList<>();
        for (Object obj : cached) {
            if (obj == null) {
                continue;
            }
            result.add(toEnemyType(obj));
        }
        return result;
    }

    public void addEnemyType(EnemyType enemyType) {
        if (enemyType == null || enemyType.getType() == null || enemyType.getType().isEmpty()) {
            throw new IllegalArgumentException("敌方类型名称(type)不能为空");
        }

        EnemyType saved = enemyTypeRepository.save(enemyType);
        redisTemplate.opsForHash().put(KEY_ENEMY_TYPES, saved.getType(), saved);
        System.out.println(">>> 已添加/更新敌方类型: " + saved.getType());
    }

    public void deleteEnemyType(String enemyType) {
        enemyTypeRepository.deleteById(enemyType);
        redisTemplate.opsForHash().delete(KEY_ENEMY_TYPES, enemyType);
    }

    private EnemyType toEnemyType(Object obj) {
        if (obj instanceof EnemyType) {
            return (EnemyType) obj;
        }
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toJavaObject(EnemyType.class);
        }
        return JSON.parseObject(JSON.toJSONString(obj), EnemyType.class);
    }
}
