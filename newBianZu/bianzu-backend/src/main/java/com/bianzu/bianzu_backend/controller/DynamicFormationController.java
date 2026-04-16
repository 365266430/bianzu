package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.FormationParadigm;
import com.bianzu.bianzu_backend.model.dto.DynamicFormationRequestDTO;
import com.bianzu.bianzu_backend.model.dto.DynamicFormationResultDTO;
import com.bianzu.bianzu_backend.service.DynamicFormationPlanningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 动态编组控制器
 */
@RestController
@RequestMapping("/formation")
public class DynamicFormationController {

    @Autowired
    private DynamicFormationPlanningService dynamicFormationPlanningService;

    @PostMapping("/dynamic/generate")
    public Result<DynamicFormationResultDTO> generateDynamicFormation(
            @RequestBody DynamicFormationRequestDTO request) {

        if (request == null) {
            return Result.failed("请求参数不能为空");
        }
        if (request.getSelectedWeaponIds() == null || request.getSelectedWeaponIds().isEmpty()) {
            return Result.failed("请选择武器装备");
        }
        if (request.getSelectedEnemyIds() == null || request.getSelectedEnemyIds().isEmpty()) {
            return Result.failed("请选择敌方目标");
        }
        if (request.getParadigm() == null) {
            return Result.failed("请选择编组范式");
        }

        DynamicFormationResultDTO result = dynamicFormationPlanningService.generateDynamicFormation(request);
        return Result.success(result, "动态编组方案生成成功");
    }

    @GetMapping("/paradigms")
    public Result<FormationParadigm[]> getParadigms() {
        return Result.success(FormationParadigm.values(), "获取编组范式成功");
    }
}
