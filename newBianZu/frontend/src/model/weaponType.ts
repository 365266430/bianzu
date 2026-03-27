// src/types/weaponType.ts
import type { FireType } from './fireType';

/**
 * 武器装备类型 (WeaponType)
 * 对应后端: WeaponType.java
 */
export interface WeaponType {
    /**
     * 类型名称
     * 后端: type
     */
    type: string;

    /**
     * 部署位置
     * 后端: deployDomain
     */
    deployDomain: string; // '地' | '海' | '天' | '空'

    /**
     * 装备作用
     * 后端: function
     * (function 是关键字，使用时建议用 object['function'] 或在模板中直接用 .function)
     */
    function: string; 

    /**
     * 通道数
     * 后端: channelCount
     */
    channelCount: number;

    /**
     * 火力配置列表
     * 后端: fireTypes (List<FireTypeAllocation>)
     */
    fireTypes?: FireTypeAllocation[];

    description: string;
}

/**
 * 内部类：火力分配
 * 后端: WeaponType.FireTypeAllocation
 */
export interface FireTypeAllocation {
    /**
     * 嵌套的火力单元类型对象
     * 后端: fireType (引用 FireType 类)
     */
    fireType: String;

    /**
     * 数量
     * 后端: quantity
     */
    quantity: number;
    
}