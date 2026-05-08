package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/zone")
public class ProtectionZoneAssignmentController {

    @Autowired
    private ProtectionZoneService zoneService;

    @PostMapping("/auto-assign-weapons")
    public List<ProtectionZone> autoAssignWeaponsToZones() {
        return zoneService.autoAssignWeaponsToZones();
    }
}
