package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.repository.WeaponNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WeaponNodeService {

    private static final String KEY_BLUE_WEAPONS = "sim:state:blue_weapons";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeaponNodeRepository weaponNodeRepository;

    @Autowired
    private WeaponTypeService weaponTypeService;

    public List<WeaponNode> getAllWeapons() {
        Object obj = redisTemplate.opsForValue().get(KEY_BLUE_WEAPONS);
        if (obj != null) {
            return JSON.parseArray(JSON.toJSONString(obj), WeaponNode.class);
        }

        List<WeaponNode> nodes = weaponNodeRepository.findAll();
        redisTemplate.opsForValue().set(KEY_BLUE_WEAPONS, nodes);
        return nodes;
    }

    public void saveAllWeapons(List<WeaponNode> nodes) {
        if (nodes == null) {
            return;
        }
        weaponNodeRepository.deleteAll();
        weaponNodeRepository.saveAll(nodes);
        redisTemplate.opsForValue().set(KEY_BLUE_WEAPONS, nodes);
    }

    public void initWeapons() {
        if (weaponTypeService.getWeaponByType("HQ-9_Launcher") == null) {
            throw new RuntimeException("请先初始化 WeaponUnit 模板库！");
        }

        List<WeaponNode> allNodes = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            allNodes.add(createWeaponInstance("HQ-9_Launcher"));
        }
        saveAllWeapons(allNodes);
    }

    public WeaponNode createWeaponInstance(String type) {
        WeaponType template = weaponTypeService.getWeaponByType(type);
        if (template == null) {
            throw new RuntimeException("未知武器类型: " + type);
        }

        WeaponNode node = new WeaponNode();
        node.setId(UUID.randomUUID().toString());
        node.setType(type);
        node.setStatus(0);

        List<WeaponNode.NodeAmmoState> ammoStates = new ArrayList<>();
        if (template.getFireTypes() != null) {
            for (WeaponType.FireTypeAllocation allocation : template.getFireTypes()) {
                ammoStates.add(new WeaponNode.NodeAmmoState(allocation.getFireType(), allocation.getQuantity()));
            }
        }
        node.setAmmoStates(ammoStates);
        return node;
    }

    public void clearAllWeapons() {
        weaponNodeRepository.deleteAll();
        redisTemplate.delete(KEY_BLUE_WEAPONS);
    }
}
