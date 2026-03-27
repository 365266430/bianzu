package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.service.FireTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fire")
public class FireController {

    @Autowired
    private FireTypeService fireTypeService;

//    // 初始化弹药库
//    // POST http://localhost:8080/fireunit/init
//    @PostMapping("/init")
//    public String init() {
//        fireTypeService.initFireUnitLibrary();
//        return "弹药库初始化成功 (HQ-9, HQ-16, 1130-Ammo)";
//    }

    // 查看弹药库列表
    // GET http://localhost:8080/fireunit/list
    @GetMapping("/list-types")
    public Result<List<FireType>> list() {
        List<FireType> list=fireTypeService.getFireTypes();
        if(list.isEmpty()||list==null)
            return Result.failed("火力类型列表为空");
        return Result.success(fireTypeService.getFireTypes());
    }


    /**
     * 添加火力类型
     */
    @PostMapping("/add-type")
    public Result<String> addFireType(@RequestBody FireType fireType) {
        try {
            fireTypeService.addFireType(fireType);
            return Result.success(fireType.getType()+"类型火力单元添加成功！");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("添加失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-type/{type}")
    public Result<String> deleteFireType(@PathVariable String type){
        try {
            fireTypeService.deleteFireType(type);
            return Result.success("删除成功");
        }catch (RuntimeException e){
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }
}