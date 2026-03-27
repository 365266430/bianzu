// src/types/fireType.ts

/**
 * 火力单元类型 (FireType)
 * 对应后端: FireType.java
 */
export interface FireType {
    /**
     * 类型名称
     * 后端: type
     */
    type: string;

    /**
     * 成本
     * 后端: cost
     */
    cost: number;

    /**
     * 拦截概率
     * 后端: interception
     */
    interception: number;

    /**
     * 最大射程
     * 后端: maxRange
     */
    maxRange: number;

    /**
     * 最小射程
     * 后端: minRange
     */
    minRange: number;

    /**
     * 最大射高
     * 后端: maxAlt
     */
    maxAlt: number;

    /**
     * 最小射高
     * 后端: minAlt
     */
    minAlt: number;

    /**
     * 调度成本
     * 后端: attCost
     */
    attCost: number;

    
    description: string;
}