package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.FormationPlanDTO;
import com.bianzu.bianzu_backend.algorithm.FormationStrategy;
import com.bianzu.bianzu_backend.service.EnemyNodeService;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Author dongjun
 * @Date 2026/3/28 13:02
 * @Param 编组生成
 */


@RestController
@RequestMapping("/formation")
public class FormationController {

    @Autowired
    private EnemyNodeService enemyNodeService;

    @Autowired
    private WeaponNodeService weaponNodeService;

    @Autowired
    private Map<String, FormationStrategy> strategyMap;

    @PostMapping("/static/generate")
    public Result<List<FormationPlanDTO>> generateStaticFormation(@RequestBody AlgorithmConfigDTO config) {
        // redis获取当前静态态势
        var enemies = enemyNodeService.getAllEnemies();
        var weapons = weaponNodeService.getAllWeapons();

        if (enemies == null || weapons == null || enemies.isEmpty() || weapons.isEmpty()) {
            return Result.failed("态势数据不足，无法生成编组");
        }

        // 根据前端选择的算法类型选取策略
        FormationStrategy strategy = strategyMap.get(config.getAlgorithmType());
        if (strategy == null) {
            return Result.failed("不支持的算法类型: " + config.getAlgorithmType());
        }

        // 执行算法生成方案
        List<FormationPlanDTO> plans = strategy.generatePlans(weapons, enemies, config);

        return Result.success(plans, "静态编组方案生成成功");
    }
}