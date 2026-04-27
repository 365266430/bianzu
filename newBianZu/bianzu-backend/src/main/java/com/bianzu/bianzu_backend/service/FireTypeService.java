package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.repository.FireTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FireTypeService {

    private static final String KEY_FIRE_UNITS = "sim:config:fire_types";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FireTypeRepository fireTypeRepository;

    @Autowired
    private WeaponTypeService weaponTypeService;

    public List<FireType> getFireTypes() {
        List<Object> cached = redisTemplate.opsForHash().values(KEY_FIRE_UNITS);
        if (cached == null || cached.isEmpty()) {
            List<FireType> dbList = fireTypeRepository.findAll();
            dbList.forEach(fireType -> redisTemplate.opsForHash().put(KEY_FIRE_UNITS, fireType.getType(), fireType));
            return dbList;
        }

        List<FireType> result = new ArrayList<>();
        for (Object obj : cached) {
            if (obj != null) {
                result.add(toFireType(obj));
            }
        }
        return result;
    }

    public FireType getFireUnitByType(String typeName) {
        Object obj = redisTemplate.opsForHash().get(KEY_FIRE_UNITS, typeName);
        if (obj != null) {
            return toFireType(obj);
        }

        FireType fireType = fireTypeRepository.findById(typeName).orElse(null);
        if (fireType != null) {
            redisTemplate.opsForHash().put(KEY_FIRE_UNITS, fireType.getType(), fireType);
        }
        return fireType;
    }

    public void addFireType(FireType fireType) {
        if (fireType == null || fireType.getType() == null || fireType.getType().isEmpty()) {
            throw new IllegalArgumentException("火力类型名称(type)不能为空");
        }
        FireType saved = fireTypeRepository.save(fireType);
        redisTemplate.opsForHash().put(KEY_FIRE_UNITS, saved.getType(), saved);
    }

    public void deleteFireType(String type) {
        if (isFireTypeInUse(type)) {
            throw new RuntimeException("无法删除：该火力类型正在被其他武器装备引用，请先解除关联。");
        }
        fireTypeRepository.deleteById(type);
        redisTemplate.opsForHash().delete(KEY_FIRE_UNITS, type);
    }

    private boolean isFireTypeInUse(String targetFireType) {
        List<WeaponType> allWeapons = weaponTypeService.getWeaponTypes();
        for (WeaponType weapon : allWeapons) {
            if (weapon.getFireTypes() == null) {
                continue;
            }
            for (WeaponType.FireTypeAllocation allocation : weapon.getFireTypes()) {
                if (targetFireType.equals(allocation.getFireType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private FireType toFireType(Object obj) {
        if (obj instanceof FireType) {
            return (FireType) obj;
        }
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toJavaObject(FireType.class);
        }
        return JSON.parseObject(JSON.toJSONString(obj), FireType.class);
    }
}
