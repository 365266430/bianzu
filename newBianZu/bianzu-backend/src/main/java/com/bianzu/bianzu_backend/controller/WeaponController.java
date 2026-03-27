package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.FireType;
import com.bianzu.bianzu_backend.model.WeaponType;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import com.bianzu.bianzu_backend.service.WeaponTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/weapon")
public class WeaponController {

    @Autowired
    private WeaponTypeService weaponTypeService;

    @Autowired
    private WeaponNodeService weaponNodeService;

    // 1. 初始化武器库
    // :/weapon/init
    // 必须确保先初始化了 FireUnit 库，否则会报错
//    @PostMapping("/init-types")
//    public String initLibrary() {
//        try {
//            weaponTypeService.initWeaponLibrary();
//            return "武器装备库初始化成功 (HQ-9, LD-2000, Radar)";
//        } catch (Exception e) {
//            return "初始化失败: " + e.getMessage();
//        }
//    }

    // 2. 查看列表
    @GetMapping("/list-types")
    public Result<List<WeaponType>> getWeaponList() {
        List<WeaponType> list=weaponTypeService.getWeaponTypes();
        if(list.isEmpty()||list==null)
            return Result.failed("武器类型列表为空！！");
        return Result.success(list);
    }


    @PostMapping("/init-nodes")
    public String initNodes(){
        weaponNodeService.initWeapons();
        return "初始化成功";
    }


    /**
     * 添加火力类型
     */
    @PostMapping("/add-type")
    public Result<String> addWeaponType(@RequestBody WeaponType weaponType) {
        try {
            weaponTypeService.addWeaponType(weaponType);
            return Result.success(weaponType.getType()+"类型武器单元添加成功！");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failed("添加失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-type/{type}")
    public Result<String> deleteWeaponType(@PathVariable String type){
        try {
            weaponTypeService.deleteWeaponType(type);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }
}