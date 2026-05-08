package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.common.WebSocketMessage;
import com.bianzu.bianzu_backend.handler.SimulationHandler;
import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.FormationParadigm;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.SimulationContext;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.processor.DynamicFormationProcessor;
import com.bianzu.bianzu_backend.processor.MovementProcessor;
import com.bianzu.bianzu_backend.processor.SimulationProcessor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimulationEngineService {
    // 1. 注入数据层 (Memory)
    @Autowired
    private EnemyNodeService eNodeService;
    @Autowired
    private ProtectionZoneService zoneService;
    @Autowired
    private WeaponNodeService weaponNodeService;

    @Autowired
    private SimulationHandler simulationHandler;



//    // 2. 注入逻辑层 (Processors)
    @Autowired
    private MovementProcessor movementProcessor;
    @Autowired
    private DynamicFormationProcessor dynamicFormationProcessor;
//    @Autowired
//    private CombatProcessor combatProcessor;

    // 处理器链表 (流水线)
    private List<SimulationProcessor> processors;

    // 初始化时组装流水线
    @PostConstruct
    public void initPipeline() {
        processors = new ArrayList<>();
        processors.add(movementProcessor);
        processors.add(dynamicFormationProcessor);
        // processors.add(combatProcessor); // 想加功能？加一行就行
    }

    // 仿真开关
    public static boolean isRunning = false;
    private int currentStep = 0;


    @Scheduled(fixedRate = 1000)
    public void tick() {
//        System.out.println("每1秒执行一次");
        if (!isRunning) return;

        // --- 1. Load (加载数据) ---
        SimulationContext ctx = loadContext();

        // --- 2. Process (执行流水线) ---
        for (SimulationProcessor processor : processors) {
            processor.process(ctx);
        }

        // --- 3. Commit (提交结果) ---
        commitContext(ctx);
    }

    // 私有辅助方法：负责加载
    private SimulationContext loadContext() {
        SimulationContext ctx = new SimulationContext();
        ctx.setEnemies(eNodeService.getAllEnemies());
        ctx.setZones(zoneService.getAllZones());
        ctx.setWeapons(weaponNodeService.getAllWeapons());
        ctx.setParadigm(FormationParadigm.ALL);
        ctx.setStep(++currentStep);
        return ctx;
    }
    // 私有辅助方法：负责保存和推送
    private void commitContext(SimulationContext ctx) {
        List<EnemyNode> enemies = ctx.getEnemies();
        List<ProtectionZone> zones = ctx.getZones();
        List<WeaponNode> weapons = ctx.getWeapons();

        // 1. 存 Redis
        eNodeService.saveAllEnemies(enemies);
        zoneService.saveAllZones(zones);
        weaponNodeService.saveAllWeapons(weapons);

        // 2. 推 WebSocket
        WebSocketMessage<List<EnemyNode>> msg = WebSocketMessage.of("ENEMY_UPDATE", enemies);
        simulationHandler.pushToFrontend(JSON.toJSONString(msg));
        WebSocketMessage<List<ProtectionZone>> zoneMsg = WebSocketMessage.of("ZONE_UPDATE", zones);
        simulationHandler.pushToFrontend(JSON.toJSONString(zoneMsg));
        WebSocketMessage<List<WeaponNode>> weaponMsg = WebSocketMessage.of("WEAPON_UPDATE", weapons);
        simulationHandler.pushToFrontend(JSON.toJSONString(weaponMsg));
    }

}
