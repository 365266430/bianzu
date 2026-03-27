package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSONArray;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 保护区服务
 * 职责：管理蓝方防空阵地 (State) - 最终的战场实体
 * 依赖：需要从 WeaponEquipmentService 获取装备信息
 * 存储：Redis String (Key: sim:state:blue_zones) -> 存 List
 */
@Service
public class ProtectionZoneService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeaponTypeService weaponService; // 注入下层服务

    // Redis Key 常量：注意这里用 state，因为它是在战场上存在的实体
    private static final String KEY_BLUE_ZONES = "sim:state:blue_zones";

    /**
     * 1. 初始化保护区
     * 组装“发射车”到“阵地”
     */
    public void initProtectionZones() {
        // 1. 从武器库获取标准装备模板
        WeaponType hq9Launcher = weaponService.getWeaponByType("HQ-9_Launcher");
        WeaponType ld2000Ciws = weaponService.getWeaponByType("LD-2000_CIWS");
        WeaponType radar = weaponService.getWeaponByType("Type-346_Radar");

        // 校验依赖是否存在
        if (hq9Launcher == null || ld2000Ciws == null) {
            throw new RuntimeException("错误：未找到武器装备数据！请先执行 Weapon 初始化。");
        }

        List<ProtectionZone> zones = new ArrayList<>();

        // --- A. 创建北京防区 (重兵把守) ---
        ProtectionZone zoneBj = new ProtectionZone();
        zoneBj.setId("Zone-Beijing");
        zoneBj.setLocation(List.of(116.40, 39.90));
        zoneBj.setSize(25000.0); // 25km 半径
        zoneBj.setValue(3);      // 最高价值
        zoneBj.setHealth(10);


        zones.add(zoneBj);


        // --- B. 创建上海防区 (侧重末端防御) ---
        ProtectionZone zoneSh = new ProtectionZone();
        zoneSh.setId("Zone-Shanghai");
        zoneSh.setLocation(List.of(121.47, 31.23));
        zoneSh.setSize(15000.0);
        zoneSh.setValue(2);
        zoneSh.setHealth(10);


        zones.add(zoneSh);


        // 2. 存入 Redis (作为战场态势，直接存 List 对象)
        redisTemplate.opsForValue().set(KEY_BLUE_ZONES, zones);
        System.out.println(">>> 蓝方保护区(ProtectionZones)初始化完毕，共部署 " + zones.size() + " 个阵地");
    }

    /**
     * 2. 获取所有保护区
     */
    public List<ProtectionZone> getAllZones() {
        Object obj = redisTemplate.opsForValue().get(KEY_BLUE_ZONES);
        if (obj == null) return new ArrayList<>();
        List<ProtectionZone> objList=((JSONArray)obj).toJavaList(ProtectionZone.class);
        return objList;
    }

    /**
     * 保存所有保护区到 Redis
     * 操作：全量覆盖
     * 场景：当保护区扣血、或者武器弹药消耗后，调用此方法更新状态
     * @param zones 最新的保护区列表
     */
    public void saveAllZones(List<ProtectionZone> zones) {
        if (zones == null) {
            return;
        }
        // Key: sim:state:blue_zones
        redisTemplate.opsForValue().set(KEY_BLUE_ZONES, zones);
    }

    /**
     * 更新指定保护区的武器分配
     * 场景：调度到达后，或者重新编组时调用
     *
     * @param zoneId 目标保护区 ID
     * @param weaponIds 该保护区现在拥有的所有武器节点 ID 列表
     * @return 是否更新成功
     */
    public boolean assignWeaponsToZone(String zoneId, List<String> weaponIds) {
        // 1. 获取当前所有 Zone (快照)
        List<ProtectionZone> allZones = getAllZones();
        if (allZones.isEmpty()) {
            System.err.println("错误：没有找到任何保护区数据");
            return false;
        }

        boolean found = false;

        // 2. 查找并更新
        for (ProtectionZone zone : allZones) {
            if (zone.getId().equals(zoneId)) {
                // 更新列表 (建议创建新 ArrayList 避免引用问题)
                zone.setStationedWeaponIds(new ArrayList<>(weaponIds));
                found = true;
                System.out.println(">>> 保护区 [" + zoneId + "] 兵力已更新，当前拥有: " + weaponIds.size() + " 个单位");
                break;
            }
        }

        if (!found) {
            System.err.println("错误：未找到 ID 为 [" + zoneId + "] 的保护区");
            return false;
        }

        // 3. 保存回 Redis (全量覆盖)
        saveAllZones(allZones);

        // 4. (可选) 如果你的前端需要实时看到变化，这里可以触发 WebSocket 推送
        // webSocketService.broadcastZoneUpdate(allZones);

        return true;
    }
}