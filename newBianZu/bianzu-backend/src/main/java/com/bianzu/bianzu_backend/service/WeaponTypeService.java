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
public class WeaponTypeService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // Redis Key 常量
    private static final String KEY_WEAPON_TYPES = "sim:config:weapon_types";

    public List<WeaponType> getWeaponTypes() {
        List<Object> rawList = redisTemplate.opsForHash().values(KEY_WEAPON_TYPES);
        List<WeaponType> result = new ArrayList<>();

        for (Object obj : rawList) {
            // FastJSON 反序列化处理
            if (obj instanceof JSONObject) {
                result.add(((JSONObject) obj).toJavaObject(WeaponType.class));
            } else if (obj instanceof WeaponType) {
                result.add((WeaponType) obj);
            }
        }
        return result;
    }

    /**
     * 3. 根据名称获取特定装备 (供 ProtectionZoneService 调用)
     */
    public WeaponType getWeaponByType(String typeName) {
        Object obj = redisTemplate.opsForHash().get(KEY_WEAPON_TYPES, typeName);
        if (obj == null) return null;

        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toJavaObject(WeaponType.class);
        }
        return (WeaponType) obj;
    }


    public void addWeaponType(WeaponType weaponType){
        if (weaponType == null || weaponType.getType() == null || weaponType.getType().isEmpty()) {
            throw new IllegalArgumentException("武器类型名称(type)不能为空");
        }
        redisTemplate.opsForHash().put(KEY_WEAPON_TYPES, weaponType.getType(), weaponType);
    }

    /**
     * 删
     */
    public void deleteWeaponType(String weaponType){
        redisTemplate.opsForHash().delete(KEY_WEAPON_TYPES,weaponType);
    }

}
