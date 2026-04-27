package com.bianzu.bianzu_backend.model;

import com.bianzu.bianzu_backend.config.NodeAmmoStateListConverter;
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
 * 武器装备节点 (WeaponNode)
 * 职责：描述战场上的一辆具体发射车/雷达车
 * 对应前端: WeaponNode
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "weapon_nodes")
public class WeaponNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符
     * 例如: "HQ9-BJ-01" (北京防区1号车)
     */
    @Id
    @Column(length = 100)
    private String id;

    /**
     * 关联的模板类型名称
     * 例如: "HQ-9_Launcher" -> 对应 WeaponEquipmentService 里的 Key
     */
    private String type;

    /**
     * 当前状态
     * 0: 待命 (Idle)
     * 1: 分配中
     * 2: 被调度分配中 (Engaging)
     */
    private Integer status;



    /**
     * 剩余弹药状态
     * 这是一个列表，因为有些武器可能挂载了多种弹药 (如: [ {导弹: 3}, {机炮: 500} ])
     */
    @Lob
    @Convert(converter = NodeAmmoStateListConverter.class)
    private List<NodeAmmoState> ammoStates = new ArrayList<>();


    /**
     * 内部类：具体的弹药状态
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NodeAmmoState implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 弹药类型名称 (e.g., "HQ-9_Missile")
         * 对应 WeaponUnit.fireTypes 里的 fireType.type
         */
        private String fireUnitType;

        /**
         * 当前剩余数量 (e.g., 3)
         * 初始值应等于 WeaponUnit 里的 quantity
         */
        private Integer currentCount;
    }

}
