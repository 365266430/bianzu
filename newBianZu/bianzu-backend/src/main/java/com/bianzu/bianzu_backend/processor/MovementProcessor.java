package com.bianzu.bianzu_backend.processor;

import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.SimulationContext;
import com.bianzu.bianzu_backend.service.EnemyTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MovementProcessor implements SimulationProcessor{

    // 仿真步长 (秒) -> 对应 @Scheduled(fixedRate=100)
    // 如果后端每 100ms 跑一次，timeStep = 0.1秒
    // 为了演示效果明显，可以适当放大这个系数 (比如 10倍速)
    private static final double TIME_STEP = 0.1 * 10.0;

    // 地球半径 (米)
    private static final double EARTH_RADIUS = 6371000.0;
    // 1 马赫 = 340 m/s
    private static final double MACH_TO_MS = 340.0;

    @Autowired
    private EnemyTypeService enemyTypeService;


    @Override
    public void process(SimulationContext ctx) {
        // 直接从 ctx 里拿数据，不需要传参了
        List<EnemyNode> enemies = ctx.getEnemies();
        List<ProtectionZone> zones = ctx.getZones();

        // 预处理 Zone Map 加速查找
        Map<String, ProtectionZone> zoneMap = zones.stream()
                .collect(Collectors.toMap(ProtectionZone::getId, z -> z));

        enemiesMove(enemies);

    }


    /**
     * 核心移动算法
     */
    private void enemiesMove(List<EnemyNode> enemies) {
        for (EnemyNode node : enemies) {
            double speedMach = node.getSpeed() != null ? node.getSpeed() : 0.0;
            double heading = node.getHeading() != null ? node.getHeading() : 0.0;

            if (speedMach <= 0) continue;

            // 1. 转换速度: 马赫 -> m/s
            double speedMs = speedMach * MACH_TO_MS;

            // 2. 计算这一帧移动的直线距离 (米)
            double distanceMeters = speedMs * TIME_STEP;

            // 3. 将距离分解为 经度/纬度 的位移 (简化为平面几何，小范围内误差可忽略)
            // 航向角: 0=北(Lat+), 90=东(Lng+), 180=南(Lat-), 270=西(Lng-)
            // Math.sin/cos 接受的是弧度
            double headingRad = Math.toRadians(heading);

            // deltaLat = d * cos(heading)
            // deltaLng = d * sin(heading)
            double dy = distanceMeters * Math.cos(headingRad); // 向北的距离 (m)
            double dx = distanceMeters * Math.sin(headingRad); // 向东的距离 (m)

            // 4. 将米转换为经纬度度数
            // 纬度 1度 ≈ 111km (111000m)
            double deltaLat = (dy / EARTH_RADIUS) * (180 / Math.PI);

            // 经度 1度 ≈ 111km * cos(lat)
            double currentLatRad = Math.toRadians(node.getLatitude());
            double deltaLng = (dx / EARTH_RADIUS) * (180 / Math.PI) / Math.cos(currentLatRad);

            // 5. 更新位置
            node.setLatitude(node.getLatitude() + deltaLat);
            node.setLongitude(node.getLongitude() + deltaLng);
        }
    }

}
