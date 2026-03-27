package com.bianzu.bianzu_backend.controller;


import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.common.ResultCode;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.service.EnemyNodeService;
import com.bianzu.bianzu_backend.service.EnemyTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enemy")
public class EnemyController {
    @Autowired
    private EnemyTypeService typeService;

    @Autowired
    private EnemyNodeService nodeService;

//    // 第一步：加载配置 (通常系统启动时自动加载，这里为了演示手动触发)
//    @PostMapping("/load-types")
//    public String loadTypes() {
//        typeService.initEnemyTypes();
//        return "类型库加载成功 (F-35, Tomahawk...)";
//    }

    @GetMapping("/list-types")
    public Result<List<EnemyType>> getEnemyTypes(){
        List<EnemyType> data=typeService.getEnemyTypes();
        if(data.isEmpty()||data==null)
            return Result.failed("敌方类型列表为空！！");
        return Result.success(data);
    }
    @GetMapping("/list")
    public String getAllEnemys(){
        nodeService.getAllEnemies();
        return "获取所有敌方节点";
    }

    /**
     * 添加敌方类型
     * POST /api/enemyunit/add
     */
    @PostMapping("/add-type")
    public Result<String> addEnemyType(@RequestBody EnemyType enemyType) {
        try {
            typeService.addEnemyType(enemyType);
            return Result.success("添加成功");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("添加失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-type/{type}")
    public Result<String> deleteEnemyType(@PathVariable String type){
        try {
            typeService.deleteEnemyType(type);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }
}
