package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zone")
public class ProtectionZoneController {

    @Autowired
    private ProtectionZoneService zoneService;

    // 1. 初始化保护区
    // 依赖顺序：FireUnit -> Weapon -> Zone
    @PostMapping("/init")
    public String initZones() {
        try {
            zoneService.initProtectionZones();
            return "蓝方保护区部署成功 (Beijing, Shanghai)";
        } catch (Exception e) {
            return "部署失败: " + e.getMessage();
        }
    }

    // 2. 获取保护区列表 (前端渲染地图圆圈用)
    @GetMapping("/list")
    public List<ProtectionZone> getZoneList() {
        return zoneService.getAllZones();
    }


    // 1. 定义DTO接收请求体（推荐：一个接口只能有一个 @RequestBody）
    @Data
    public static class AssignWeaponsDTO {
        private String zoneId; // 区域ID
        private List<String> weaponIds; // 武器ID列表
    }

    @PostMapping("/assign-weapon")
    public String assignWeaponsToZone(@RequestBody AssignWeaponsDTO assignWeaponsDTO){
        zoneService.assignWeaponsToZone(assignWeaponsDTO.zoneId,assignWeaponsDTO.weaponIds);
        return "武器分配成功";
    }
}