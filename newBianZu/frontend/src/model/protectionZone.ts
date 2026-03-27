// src/types/zone.ts

/**
 * 保护区 (ProtectionZone)
 * 对应后端: ProtectionZone.java
 */
export interface ProtectionZone {
    /**
     * 保护区编号或名称
     * 后端: id
     */
    id: string | number;

    /**
     * 坐标 [经度, 纬度]
     * 后端: location
     */
    location: [number, number];

    /**
     * 半径大小 (米)
     * 后端: size
     */
    size: number;

    /**
     * 价值等级 (1-3)
     * 后端: value
     */
    value: 1 | 2 | 3;

    /**
     * 健康度 (1-10)
     * 后端: health
     */
    health: number;

    /**
     * 关联的武器装备 ID 列表
     * 后端: stationedWeaponIds
     * 前端拿到这个列表后，需要去 WeaponNode 列表中查找对应的实体
     */
    stationedWeaponIds: string[];
}