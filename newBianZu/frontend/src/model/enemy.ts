// src/types/enemyNode.ts


export interface EnemyNode {
    /** 唯一标识符 */
    id: string;

    /** 类型名称 (引用 EnemyType) */
    type: string;

    /** 经度 */
    longitude: number;

    /** 纬度 */
    latitude: number;

    /** 飞行高度 (m) */
    altitude: number;

    /** 航向角 (0-360度) */
    heading: number;

    /** 当前实时速度 (马赫) */
    speed: number;
}