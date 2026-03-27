package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSONObject;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.WeaponType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FireTypeService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis Key 常量
    private static final String KEY_FIRE_UNITS = "sim:config:fire_types";


    /**
     * 2. 获取所有弹药类型
     */
    public List<FireType> getFireTypes() {
        List<Object> rawList = redisTemplate.opsForHash().values(KEY_FIRE_UNITS);
        List<FireType> result = new ArrayList<>();

        for (Object obj : rawList) {
            if (obj instanceof FireType) {
                result.add((FireType) obj);
            } else if (obj instanceof JSONObject) {
                // 处理 FastJSON 反序列化的通用对象
                result.add(((JSONObject) obj).toJavaObject(FireType.class));
            }
        }
        return result;
    }


    /**
     * 3. 根据名称获取特定弹药 (给上层 WeaponEquipmentService 调用)
     */
    public FireType getFireUnitByType(String typeName) {
        Object obj = redisTemplate.opsForHash().get(KEY_FIRE_UNITS, typeName);
        if (obj == null) return null;

        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toJavaObject(FireType.class);
        }
        return (FireType) obj;
    }

    /**
     *增
     */
    public void addFireType(FireType fireType){
        if (fireType == null || fireType.getType() == null || fireType.getType().isEmpty()) {
            throw new IllegalArgumentException("火力类型名称(type)不能为空");
        }
        redisTemplate.opsForHash().put(KEY_FIRE_UNITS, fireType.getType(), fireType);
    }

    /**
     *删
     */
    public void deleteFireType(String type){
        // 1. 检查引用
        if (isFireTypeInUse(type)) {
            throw new RuntimeException("无法删除：该火力类型正在被其他武器装备引用！请先解除关联。");
        }
        redisTemplate.opsForHash().delete(KEY_FIRE_UNITS,type);
    }

    @Autowired
    private WeaponTypeService weaponTypeService;

    // 辅助检查方法
    private boolean isFireTypeInUse(String targetFireType) {
        // 获取所有武器类型
        List<WeaponType> allWeapons = weaponTypeService.getWeaponTypes();

        for (WeaponType weapon : allWeapons) {
            if (weapon.getFireTypes() != null) {
                for (WeaponType.FireTypeAllocation allocation : weapon.getFireTypes()) {
                    // 假设现在 allocation 里存的是 String fireTypeName (引用)
                    if (targetFireType.equals(allocation.getFireType())) {
                        return true; // 发现被引用
                    }
                }
            }
        }
        return false;
    }
}

