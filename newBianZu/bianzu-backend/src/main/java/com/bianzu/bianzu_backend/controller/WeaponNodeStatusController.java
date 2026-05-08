package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/weapon")
public class WeaponNodeStatusController {

    @Autowired
    private WeaponNodeService weaponNodeService;

    @Data
    public static class WeaponStatusDTO {
        private Integer status;
    }

    @PutMapping("/node/{weaponId}/status")
    public Result<List<WeaponNode>> updateWeaponStatus(
            @PathVariable String weaponId,
            @RequestBody WeaponStatusDTO statusDTO) {
        try {
            List<WeaponNode> nodes = weaponNodeService.updateWeaponStatus(weaponId, statusDTO.getStatus());
            return Result.success(nodes, "Weapon status updated");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        }
    }

    @PutMapping("/nodes/status")
    public Result<List<WeaponNode>> updateAllWeaponStatus(@RequestBody WeaponStatusDTO statusDTO) {
        try {
            List<WeaponNode> nodes = weaponNodeService.updateAllWeaponStatus(statusDTO.getStatus());
            return Result.success(nodes, "All weapon statuses updated");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        }
    }
}
