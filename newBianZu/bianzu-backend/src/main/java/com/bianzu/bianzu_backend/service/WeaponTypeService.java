package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.repository.WeaponTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeaponTypeService {

    private static final String KEY_WEAPON_TYPES = "sim:config:weapon_types";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeaponTypeRepository weaponTypeRepository;

    public List<WeaponType> getWeaponTypes() {
        List<Object> cached = redisTemplate.opsForHash().values(KEY_WEAPON_TYPES);
        if (cached == null || cached.isEmpty()) {
            List<WeaponType> dbList = weaponTypeRepository.findAll();
            dbList.forEach(weaponType -> redisTemplate.opsForHash().put(KEY_WEAPON_TYPES, weaponType.getType(), weaponType));
            return dbList;
        }

        List<WeaponType> result = new ArrayList<>();
        for (Object obj : cached) {
            if (obj != null) {
                result.add(toWeaponType(obj));
            }
        }
        return result;
    }

    public WeaponType getWeaponByType(String typeName) {
        Object obj = redisTemplate.opsForHash().get(KEY_WEAPON_TYPES, typeName);
        if (obj != null) {
            return toWeaponType(obj);
        }

        WeaponType weaponType = weaponTypeRepository.findById(typeName).orElse(null);
        if (weaponType != null) {
            redisTemplate.opsForHash().put(KEY_WEAPON_TYPES, weaponType.getType(), weaponType);
        }
        return weaponType;
    }

    public void addWeaponType(WeaponType weaponType) {
        if (weaponType == null || weaponType.getType() == null || weaponType.getType().isEmpty()) {
            throw new IllegalArgumentException("武器类型名称(type)不能为空");
        }
        WeaponType saved = weaponTypeRepository.save(weaponType);
        redisTemplate.opsForHash().put(KEY_WEAPON_TYPES, saved.getType(), saved);
    }

    public void deleteWeaponType(String weaponType) {
        weaponTypeRepository.deleteById(weaponType);
        redisTemplate.opsForHash().delete(KEY_WEAPON_TYPES, weaponType);
    }

    private WeaponType toWeaponType(Object obj) {
        if (obj instanceof WeaponType) {
            return (WeaponType) obj;
        }
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toJavaObject(WeaponType.class);
        }
        return JSON.parseObject(JSON.toJSONString(obj), WeaponType.class);
    }
}
