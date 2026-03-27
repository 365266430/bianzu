// src/model/weaponNode.ts

/**
 * 武器装备节点 (WeaponNode)
 * 职责：描述战场上具体的实体（如某辆车）
 * 对应后端: WeaponNode.java
 */
export interface WeaponNode {
    /**
     * 唯一标识符
     * 后端: id
     */
    id: string;

    /**
     * 类型名称 (关联 WeaponType)
     * 后端: type
     */
    type: string;

    /**
     * 当前状态
     * 0: 待命 (Idle)
     * 1: 分配中
     * 2: 被调度分配中 (Engaging)
     * 后端: status
     */
    status: number;

    /**
     * 弹药状态列表
     * 后端: ammoStates
     */
    ammoStates: NodeAmmoState[];
}

/**
 * 内部类：具体的弹药状态
 * 后端: WeaponNode.NodeAmmoState
 */
export interface NodeAmmoState {
    /**
     * 弹药类型名称 (关联 FireType)
     * 后端: fireUnitType
     */
    fireUnitType: string;

    /**
     * 当前剩余数量
     * 后端: currentCount
     */
    currentCount: number;
}