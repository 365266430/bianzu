package com.bianzu.bianzu_backend.algorithm;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.StaticFormationResultDTO;

import java.util.List;

public interface FormationStrategy {
    StaticFormationResultDTO generatePlans(
            List<WeaponNode> weapons,
            List<EnemyNode> enemies,
            List<ProtectionZone> zones,
            AlgorithmConfigDTO config);
}
