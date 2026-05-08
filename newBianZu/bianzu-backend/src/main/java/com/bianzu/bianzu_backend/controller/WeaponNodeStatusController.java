package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weapon")
public class WeaponNodeStatusController {

    @Autowired
    private WeaponNodeService weaponNodeService;

    @Autowired
    private ProtectionZoneService protectionZoneService;

    @Data
    public static class WeaponStatusDTO {
        private Integer status;
    }

    @Data
    public static class CreateWeaponNodesDTO {
        private String type;
        private Integer count;
        private Integer status;
        private String zoneId;
        private List<WeaponNode.NodeAmmoState> ammoStates;
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

    @PostMapping("/nodes")
    public Result<Map<String, Object>> createWeaponNodes(@RequestBody CreateWeaponNodesDTO request) {
        try {
            int count = request.getCount() == null ? 1 : request.getCount();
            List<WeaponNode> created = weaponNodeService.createWeaponNodes(
                    request.getType(),
                    count,
                    request.getStatus(),
                    request.getAmmoStates());
            boolean assignedToZone = false;
            if (request.getZoneId() != null && !request.getZoneId().isBlank()) {
                assignedToZone = protectionZoneService.addWeaponsToZone(
                        request.getZoneId(),
                        created.stream().map(WeaponNode::getId).toList());
            }
            return Result.success(Map.of(
                    "created", created,
                    "assignedToZone", assignedToZone
            ), "Weapon nodes created");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        }
    }

    @DeleteMapping("/node/{weaponId}")
    public Result<List<WeaponNode>> deleteWeaponNode(@PathVariable String weaponId) {
        try {
            List<WeaponNode> nodes = weaponNodeService.deleteWeaponNode(weaponId);
            protectionZoneService.removeWeaponFromZones(weaponId);
            return Result.success(nodes, "Weapon node deleted");
        } catch (IllegalArgumentException e) {
            return Result.failed(e.getMessage());
        }
    }
}
