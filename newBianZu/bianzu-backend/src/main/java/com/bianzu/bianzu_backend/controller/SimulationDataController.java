package com.bianzu.bianzu_backend.controller;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.common.WebSocketMessage;
import com.bianzu.bianzu_backend.handler.SimulationHandler;
import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.service.EnemyNodeService;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sim")
public class SimulationDataController {

    @Autowired
    private EnemyNodeService enemyNodeService;

    @Autowired
    private SimulationHandler simulationHandler;

    @Autowired
    private ProtectionZoneService protectionZoneService;

    @PostMapping("/add-enemy")
    public Result<EnemyNode> addEnemyNode(@RequestBody EnemyNode enemyNode) {
        try {
            List<EnemyNode> enemies = enemyNodeService.appendEnemy(enemyNode);
            pushEnemyUpdate(enemies);
            return Result.success(enemyNode, "Enemy target created successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to create enemy target: " + e.getMessage());
        }
    }

    @PutMapping("/enemy/{enemyId}")
    public Result<EnemyNode> updateEnemyNode(@PathVariable String enemyId, @RequestBody EnemyNode enemyNode) {
        try {
            List<EnemyNode> enemies = enemyNodeService.updateEnemy(enemyId, enemyNode);
            pushEnemyUpdate(enemies);
            return Result.success(enemyNode, "Enemy target updated successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to update enemy target: " + e.getMessage());
        }
    }

    @DeleteMapping("/enemy/{enemyId}")
    public Result<String> deleteEnemyNode(@PathVariable String enemyId) {
        try {
            List<EnemyNode> enemies = enemyNodeService.removeEnemyById(enemyId);
            pushEnemyUpdate(enemies);
            return Result.success("ok", "Enemy target deleted successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to delete enemy target: " + e.getMessage());
        }
    }

    @PostMapping("/add-zone")
    public Result<ProtectionZone> addProtectionZone(@RequestBody ProtectionZone zone) {
        try {
            List<ProtectionZone> zones = protectionZoneService.appendZone(zone);
            pushZoneUpdate(zones);
            return Result.success(zone, "Protection zone created successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to create protection zone: " + e.getMessage());
        }
    }

    @PutMapping("/zone/{zoneId}")
    public Result<ProtectionZone> updateProtectionZone(@PathVariable String zoneId, @RequestBody ProtectionZone zone) {
        try {
            List<ProtectionZone> zones = protectionZoneService.updateZone(zoneId, zone);
            pushZoneUpdate(zones);
            return Result.success(zone, "Protection zone updated successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to update protection zone: " + e.getMessage());
        }
    }

    @DeleteMapping("/zone/{zoneId}")
    public Result<String> deleteProtectionZone(@PathVariable String zoneId) {
        try {
            List<ProtectionZone> zones = protectionZoneService.removeZoneById(zoneId);
            pushZoneUpdate(zones);
            return Result.success("ok", "Protection zone deleted successfully");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to delete protection zone: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear-all")
    public Result<String> clearAllObjects() {
        try {
            pushEnemyUpdate(enemyNodeService.clearEnemies());
            pushZoneUpdate(protectionZoneService.clearZones());
            return Result.success("ok", "All map objects cleared successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("Failed to clear map objects: " + e.getMessage());
        }
    }

    private void pushEnemyUpdate(List<EnemyNode> enemies) {
        simulationHandler.pushToFrontend(JSON.toJSONString(WebSocketMessage.of("ENEMY_UPDATE", enemies)));
    }

    private void pushZoneUpdate(List<ProtectionZone> zones) {
        simulationHandler.pushToFrontend(JSON.toJSONString(WebSocketMessage.of("ZONE_UPDATE", zones)));
    }
}
