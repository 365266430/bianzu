package com.bianzu.bianzu_backend.model.dto;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshotDTO {
    private List<WeaponNode> weaponNodes;
    private List<EnemyNode> enemyNodes;
    private List<ProtectionZone> zones;
}
