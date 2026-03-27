package com.bianzu.bianzu_backend.model;

import lombok.Data;
import java.util.List;

/**
 * 仿真上下文：承载当前这一帧（Tick）的所有状态数据
 */
@Data
public class SimulationContext {
    private List<EnemyNode> enemies;
    private List<ProtectionZone> zones;
}
