package com.bianzu.bianzu_backend.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 武器装备实体类
 * 对应前端: WeaponEquipment
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeaponType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型名称或编号
     * 前端: Type (string | number)
     */
    private String type;

    /**
     * 部署位置（地/海/天/空）
     * 前端: DeployDomain ('地' | '海' | '天' | '空')
     * 建议：虽然可以用 Java Enum，但为了直接存取中文字符串方便，这里使用 String
     */
    private String deployDomain;

    /**
     * 装备作用（载弹、雷达等）
     * 前端: Function ('载弹' | '雷达' | string)
     */
    private String function;

    /**
     * 火力资源类型及数量
     * 前端: FireTypes (Array<{ fireType: FireUnit; quantity: number; }>)
     */
    private List<FireTypeAllocation> fireTypes = new ArrayList<>();

    /**
     * 通道数量（同时可打击或侦察敌方的数量）
     * 前端: ChannelCount
     */
    private Integer channelCount;

    private String description;

    /**
     * 内部类：用于映射 FireTypes 数组中的元素
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FireTypeAllocation implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 嵌套的 FireUnit 对象
         * 前端: fireType
         */
        private String fireType;

        /**
         * 数量
         * 前端: quantity
         */
        private Integer quantity;
    }
}
