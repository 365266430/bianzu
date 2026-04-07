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
 * 贪心策略
 */
@Component("greedyFormationStrategy")
public class GreedyFormationStrategy implements FormationStrategy {

    @Autowired
    private StaticFormationPlanningService planningService;

    @Override
    public StaticFormationResultDTO generatePlans(List<WeaponNode> weapons,
                                                  List<EnemyNode> enemies,
                                                  List<ProtectionZone> zones,
                                                  AlgorithmConfigDTO config) {
        return planningService.buildReport("greedyFormationStrategy", weapons, enemies, zones, config);
    }
}
