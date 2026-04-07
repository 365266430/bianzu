package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.algorithm.FormationStrategy;
import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.StaticFormationResultDTO;
import com.bianzu.bianzu_backend.service.EnemyNodeService;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
/**
 * 静态编组控制器
 */

@RestController
@RequestMapping("/formation")
public class FormationController {

    @Autowired
    private EnemyNodeService enemyNodeService;

    @Autowired
    private WeaponNodeService weaponNodeService;

    @Autowired
    private ProtectionZoneService protectionZoneService;

    @Autowired
    private Map<String, FormationStrategy> strategyMap;

    @PostMapping("/static/generate")
    public Result<StaticFormationResultDTO> generateStaticFormation(@RequestBody(required = false) AlgorithmConfigDTO config) {
        AlgorithmConfigDTO safeConfig = config == null ? new AlgorithmConfigDTO() : config;
        if (safeConfig.getAlgorithmType() == null || safeConfig.getAlgorithmType().isBlank()) {
            safeConfig.setAlgorithmType("psoFormationStrategy");
        }

        var enemies = enemyNodeService.getAllEnemies();
        var weapons = weaponNodeService.getAllWeapons();
        var zones = protectionZoneService.getAllZones();

        if (enemies == null || enemies.isEmpty() || weapons == null || weapons.isEmpty()) {
            return Result.failed("当前静态态势数据不足，无法生成联合编组方案。");
        }

        FormationStrategy strategy = strategyMap.get(safeConfig.getAlgorithmType());
        if (strategy == null) {
            return Result.failed("不支持的算法类型: " + safeConfig.getAlgorithmType());
        }

        StaticFormationResultDTO report = strategy.generatePlans(weapons, enemies, zones, safeConfig);
        return Result.success(report, "静态编组方案生成成功");
    }
}
