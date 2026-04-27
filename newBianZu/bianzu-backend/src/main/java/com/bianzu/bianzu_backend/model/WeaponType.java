package com.bianzu.bianzu_backend.model;


import com.bianzu.bianzu_backend.config.FireTypeAllocationListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "weapon_types")
public class WeaponType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型名称或编号
     * 前端: Type (string | number)
     */
    @Id
    @Column(length = 100)
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
    @Column(name = "weapon_function")
    private String function;

    /**
     * 火力资源类型及数量
     * 前端: FireTypes (Array<{ fireType: FireUnit; quantity: number; }>)
     */
    @Lob
    @Convert(converter = FireTypeAllocationListConverter.class)
    private List<FireTypeAllocation> fireTypes = new ArrayList<>();

    /**
     * 通道数量（同时可打击或侦察敌方的数量）
     * 前端: ChannelCount
     */
    private Integer channelCount;

    @Column(length = 1000)
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
