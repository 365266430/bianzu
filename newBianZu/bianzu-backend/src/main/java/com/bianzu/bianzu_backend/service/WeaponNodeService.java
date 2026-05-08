package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.repository.WeaponNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<WeaponNode> updateWeaponStatus(String weaponId, Integer status) {
        validateStatus(status);
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("Weapon id cannot be empty");
        }

        List<WeaponNode> nodes = new ArrayList<>(getAllWeapons());
        boolean updated = false;
        for (WeaponNode node : nodes) {
            if (weaponId.equals(node.getId())) {
                node.setStatus(status);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("Weapon not found: " + weaponId);
        }

        saveAllWeapons(nodes);
        return nodes;
    }

    public List<WeaponNode> updateAllWeaponStatus(Integer status) {
        validateStatus(status);
        List<WeaponNode> nodes = new ArrayList<>(getAllWeapons());
        for (WeaponNode node : nodes) {
            node.setStatus(status);
        }
        saveAllWeapons(nodes);
        return nodes;
    }

    public List<WeaponNode> deleteWeaponNode(String weaponId) {
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("Weapon id cannot be empty");
        }
        List<WeaponNode> nodes = new ArrayList<>(getAllWeapons());
        boolean removed = nodes.removeIf(node -> weaponId.equals(node.getId()));
        if (!removed) {
            throw new IllegalArgumentException("Weapon not found: " + weaponId);
        }
        saveAllWeapons(nodes);
        return nodes;
    }

    public void initWeapons() {
        if (weaponTypeService.getWeaponByType("HQ-9_Launcher") == null) {
            throw new RuntimeException("请先初始化 WeaponUnit 模板库！");
        }

        List<WeaponNode> allNodes = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            WeaponNode node = createWeaponInstance("HQ-9_Launcher");
            node.setId(nextWeaponNodeId("HQ-9_Launcher", allNodes));
            allNodes.add(node);
        }
        saveAllWeapons(allNodes);
    }

    public WeaponNode createWeaponInstance(String type) {
        WeaponType template = weaponTypeService.getWeaponByType(type);
        if (template == null) {
            throw new RuntimeException("未知武器类型: " + type);
        }

        WeaponNode node = new WeaponNode();
        node.setId(type);
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

    public List<WeaponNode> createWeaponNodes(String type, int count, Integer status, List<WeaponNode.NodeAmmoState> ammoStates) {
        int initialStatus = status == null ? 0 : status;
        validateStatus(initialStatus);
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Weapon type cannot be empty");
        }
        if (count <= 0 || count > 100) {
            throw new IllegalArgumentException("Weapon node count must be between 1 and 100");
        }

        List<WeaponNode> nodes = new ArrayList<>(getAllWeapons());
        List<WeaponNode> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WeaponNode node = createWeaponInstance(type);
            node.setId(nextWeaponNodeId(type, nodes));
            node.setStatus(initialStatus);
            if (ammoStates != null && !ammoStates.isEmpty()) {
                node.setAmmoStates(copyAmmoStates(ammoStates));
            }
            nodes.add(node);
            created.add(node);
        }
        saveAllWeapons(nodes);
        return created;
    }

    private List<WeaponNode.NodeAmmoState> copyAmmoStates(List<WeaponNode.NodeAmmoState> source) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (WeaponNode.NodeAmmoState ammoState : source) {
            if (ammoState == null || ammoState.getFireUnitType() == null || ammoState.getFireUnitType().isBlank()) {
                continue;
            }
            int count = ammoState.getCurrentCount() == null ? 0 : Math.max(ammoState.getCurrentCount(), 0);
            merged.put(ammoState.getFireUnitType(), count);
        }
        List<WeaponNode.NodeAmmoState> copied = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            copied.add(new WeaponNode.NodeAmmoState(entry.getKey(), entry.getValue()));
        }
        return copied;
    }

    private String nextWeaponNodeId(String type, List<WeaponNode> existingNodes) {
        String baseId = type == null || type.isBlank() ? "WeaponNode" : type.trim();
        if (!containsWeaponId(baseId, existingNodes)) {
            return baseId;
        }
        int sequence = 2;
        while (containsWeaponId(baseId + "-" + sequence, existingNodes)) {
            sequence++;
        }
        return baseId + "-" + sequence;
    }

    private boolean containsWeaponId(String id, List<WeaponNode> existingNodes) {
        for (WeaponNode node : existingNodes) {
            if (node != null && id.equals(node.getId())) {
                return true;
            }
        }
        return false;
    }

    public void clearAllWeapons() {
        weaponNodeRepository.deleteAll();
        redisTemplate.delete(KEY_BLUE_WEAPONS);
    }

    private void validateStatus(Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new IllegalArgumentException("Weapon status must be 0, 1, or 2");
        }
    }
}
