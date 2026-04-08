package com.bianzu.bianzu_backend.algorithm;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.StaticFormationResultDTO;
import com.bianzu.bianzu_backend.service.StaticFormationPlanningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * @Author dongjun
 * @Date 2026/4/8 15:08
 *遗传算法
 */
@Component("geneticFormationStrategy")
public class GeneticFormationStrategy implements FormationStrategy {

    @Autowired
    private StaticFormationPlanningService planningService;

    @Override
    public StaticFormationResultDTO generatePlans(List<WeaponNode> weapons,
                                                  List<EnemyNode> enemies,
                                                  List<ProtectionZone> zones,
                                                  AlgorithmConfigDTO config) {
        return planningService.buildReport("geneticFormationStrategy", weapons, enemies, zones, config);
    }
}
