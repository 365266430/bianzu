// src/types/enemyType.ts

/**
 * 敌方单位类型字典 (EnemyType)
 * 职责：定义情报库中已知目标的静态性能参数
 * 对应后端: EnemyType.java
 */
export interface EnemyType {
    /**
     * 类型名称 (e.g. "F-35")
     * 后端: type
     */
    type: string;

    /**
     * 目标分类 (e.g. "FIGHTER", "MISSILE")
     * 后端: category
     */
    category: string;

    /**
     * 目标价值 (威胁基数)
     * 后端: value
     */
    value: number;

    /**
     * 最大飞行速度 (马赫)
     * 后端: maxSpeed
     */
    maxSpeed: number;

    /**
     * 典型巡航高度 (m)
     * 后端: typicalAltitude
     */
    typicalAltitude: number;

    /**
     * 雷达散射截面 (m²)
     * 后端: rcs
     */
    rcs: number;

    /**
     * 最大打击半径 / 威胁半径 (m)
     * 后端: maxAttackRange
     */
    maxAttackRange: number;

    /**
     * 杀伤力 / 威力指数
     * 后端: damageCapability
     */
    damageCapability: number;

    /**
     * 备注信息
     * 后端: description
     */
    description: string;
}