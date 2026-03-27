package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.WeaponType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class WeaponNodeService {

    private static final String KEY_BLUE_WEAPONS = "sim:state:blue_weapons";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeaponTypeService weaponTypeService; // 依赖模板服务

    @Autowired
    private FireTypeService fireTypeService;

    /**
     * 获取所有武器节点 (用于仿真引擎计算)
     */
    public List<WeaponNode> getAllWeapons() {
        Object obj = redisTemplate.opsForValue().get(KEY_BLUE_WEAPONS);
        if (obj == null) return new ArrayList<>();

        // 处理 JSONArray 转 List
        String json = JSON.toJSONString(obj);
        return JSON.parseArray(json, WeaponNode.class);
    }

    /**
     * 批量保存/更新节点 (用于仿真引擎回写)
     */
    public void saveAllWeapons(List<WeaponNode> nodes) {
        // 直接存 List，变成 JSON 字符串
        redisTemplate.opsForValue().set(KEY_BLUE_WEAPONS, nodes);
    }


    /**
    * 初始武器装备节点
    */

     public void initWeapons(){
         if(weaponTypeService.getWeaponByType("HQ-9_Launcher")==null)
             throw new RuntimeException("请先初始化 WeaponUnit 模板库！");
         List<WeaponNode> allNodes = new ArrayList<>();
         for(int i=1;i<=2;i++){
             WeaponNode node = createWeaponInstance("HQ-9_Launcher");
             allNodes.add(node);
         }
         saveAllWeapons(allNodes);
     }


    /**
     * 【工厂方法】创建一个新的武器节点实例
     * 根据 type 从模板库复制弹药数据
     * @param type 武器类型 (e.g. "HQ-9_Launcher")
     * @return 初始化好的 WeaponNode
     */
    public WeaponNode createWeaponInstance(String type) {
        // 1. 查模板
        WeaponType template = weaponTypeService.getWeaponByType(type);
        if (template == null) {
            throw new RuntimeException("未知武器类型: " + type);
        }

        // 2. 创建实例
        WeaponNode node = new WeaponNode();
        node.setId(UUID.randomUUID().toString()); // 生成唯一ID
        node.setType(type);
        node.setStatus(0); // 默认待命 (Idle)

        // 3. 初始化弹药状态 (深拷贝)
        List<WeaponNode.NodeAmmoState> ammoStates = new ArrayList<>();

        if (template.getFireTypes() != null) {
            for (WeaponType.FireTypeAllocation allocation : template.getFireTypes()) {
                String fireTypeName = allocation.getFireType();
                Integer maxCount = allocation.getQuantity();
                // 填入当前状态
                ammoStates.add(new WeaponNode.NodeAmmoState(fireTypeName, maxCount));
            }
        }
        node.setAmmoStates(ammoStates);
        return node;
    }

    /**
     * 清空所有节点 (重置战场用)
     */
    public void clearAllWeapons() {
        redisTemplate.delete(KEY_BLUE_WEAPONS);
    }
}
