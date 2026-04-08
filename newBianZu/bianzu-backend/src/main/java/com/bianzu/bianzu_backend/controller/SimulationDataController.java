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
/**
 * @Author dongjun
 * @Date  15:05 2026/4/8
 */

@RestController
@RequestMapping("/sim")
public class SimulationDataController {

    @Autowired
    private EnemyNodeService enemyNodeService;

    @Autowired
    private SimulationHandler simulationHandler;

    @Autowired
    private ProtectionZoneService protectionZoneService;

    /**
     * 添加敌方节点到仿真系统中
     * <p>
     * 该接口用于创建新的敌方目标节点，创建成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param enemyNode 敌方节点对象，包含节点的所有属性信息（如ID、位置、速度等）
     * @return Result<EnemyNode> 操作结果封装对象
     *         - 成功时返回创建的敌方节点信息和成功消息
     *         - 失败时返回错误消息（参数非法或其他异常）
     */
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
    /**
     * 更新仿真系统中的敌方节点信息
     * <p>
     * 该接口用于根据指定的敌方节点ID更新其属性信息，更新成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param enemyId 敌方节点的唯一标识符，用于定位需要更新的节点
     * @param enemyNode 敌方节点对象，包含更新后的属性信息（如位置、速度等）
     * @return Result<EnemyNode> 操作结果封装对象
     *         - 成功时返回更新后的敌方节点信息和成功消息
     *         - 失败时返回错误消息（参数非法、节点不存在或其他异常）
     */
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
    /**
     * 从仿真系统中删除指定的敌方节点
     * <p>
     * 该接口用于根据敌方节点ID删除对应的节点，删除成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param enemyId 敌方节点的唯一标识符，用于定位需要删除的节点
     * @return Result<String> 操作结果封装对象
     *         - 成功时返回"ok"和成功消息
     *         - 失败时返回错误消息（参数非法、节点不存在或其他异常）
     */
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
    /**
     * 添加保护区到仿真系统中
     * <p>
     * 该接口用于创建新的保护区，创建成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param zone 保护区对象，包含保护区的所有属性信息（如ID、区域范围、类型等）
     * @return Result<ProtectionZone> 操作结果封装对象
     *         - 成功时返回创建的保护区信息和成功消息
     *         - 失败时返回错误消息（参数非法或其他异常）
     */
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
    /**
     * 更新仿真系统中的保护区信息
     * <p>
     * 该接口用于根据指定的保护区ID更新其属性信息，更新成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param zoneId 保护区的唯一标识符，用于定位需要更新的保护区
     * @param zone 保护区对象，包含更新后的属性信息（如区域范围、类型等）
     * @return Result<ProtectionZone> 操作结果封装对象
     *         - 成功时返回更新后的保护区信息和成功消息
     *         - 失败时返回错误消息（参数非法、保护区不存在或其他异常）
     */
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
    /**
     * 从仿真系统中删除指定的保护区
     * <p>
     * 该接口用于根据保护区ID删除对应的保护区，删除成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @param zoneId 保护区的唯一标识符，用于定位需要删除的保护区
     * @return Result<String> 操作结果封装对象
     *         - 成功时返回"ok"和成功消息
     *         - 失败时返回错误消息（参数非法、保护区不存在或其他异常）
     */
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
    /**
     * 清空仿真系统中的所有地图对象
     * <p>
     * 该接口用于删除所有的敌方节点和保护区，清空操作成功后会通过WebSocket推送更新给前端
     * </p>
     *
     * @return Result<String> 操作结果封装对象
     *         - 成功时返回"ok"和成功消息
     *         - 失败时返回错误消息（清除过程中的任何异常）
     */
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
    /**
     * 推送敌方节点更新信息到前端
     * <p>
     * 该方法将敌方节点列表封装成WebSocket消息并推送到前端，用于实时同步敌方节点的变更状态
     * </p>
     *
     * @param enemies 敌方节点列表，包含所有需要推送到前端的敌方节点信息
     */
    private void pushEnemyUpdate(List<EnemyNode> enemies) {
        simulationHandler.pushToFrontend(JSON.toJSONString(WebSocketMessage.of("ENEMY_UPDATE", enemies)));
    }
    /**
     * 推送保护区更新信息到前端
     * <p>
     * 该方法将保护区列表封装成WebSocket消息并推送到前端，用于实时同步保护区的变更状态
     * </p>
     *
     * @param zones 保护区列表，包含所有需要推送到前端的保护区信息
     */
    private void pushZoneUpdate(List<ProtectionZone> zones) {
        simulationHandler.pushToFrontend(JSON.toJSONString(WebSocketMessage.of("ZONE_UPDATE", zones)));
    }
}
