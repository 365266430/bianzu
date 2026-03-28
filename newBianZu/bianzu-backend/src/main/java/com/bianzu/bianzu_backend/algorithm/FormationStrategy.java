package com.bianzu.bianzu_backend.algorithm;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.model.dto.AlgorithmConfigDTO;
import com.bianzu.bianzu_backend.model.dto.FormationPlanDTO;

import java.util.List;

public interface FormationStrategy {
    //执行静态编组生成算法
    List<FormationPlanDTO> generatePlans(
            List<WeaponNode> weapons,
            List<EnemyNode> enemies,
            AlgorithmConfigDTO config);
}
