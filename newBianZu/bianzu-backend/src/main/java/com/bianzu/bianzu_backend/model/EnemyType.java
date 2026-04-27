package com.bianzu.bianzu_backend.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * 敌方单位实体类
 * 对应前端: EnemyUnit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor // 生成全参构造器 (不包含业务逻辑，仅赋值)
@Entity
@Table(name = "enemy_types")
public class EnemyType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型名称 (主键)
     * e.g., "F-35", "Tomahawk", "H-6K"
     */
    @Id
    @Column(length = 100)
    private String type;

    /**
     * 目标大类分类
     * e.g., "FIGHTER"(战斗机), "MISSILE"(导弹), "UAV"(无人机), "BOMBER"(轰炸机)
     * 用于战术规则过滤（比如近防炮优先打 MISSILE）
     */
    private String category;

    /**
     * 目标价值 (威胁评分基数)
     * e.g., 100.0 (高), 10.0 (低)
     * 击落该目标的收益，或者漏掉该目标的代价
     */
    private Double value;

    // --- 运动学性能 (Kinematics) ---

    /**
     * 最大飞行速度 (马赫)
     * 用于计算最早到达时间 (Earliest TTI)
     */
    private Double maxSpeed;

    /**
     * 典型巡航高度 (米)
     * 仅做参考，实际高度看 EnemyNode
     */
    private Double typicalAltitude;

    // --- 信号特征 (Signature) ---

    /**
     * 雷达散射截面 (RCS, m²)
     * 决定了我方雷达在多远能发现它
     */
    private Double rcs;

    // --- 攻击能力 (Capabilities) ---

    /**
     * 最大打击半径 (米) / 威胁半径
     * e.g., 轰炸机挂载巡航弹可能达到 1000km
     * 如果 距离 < maxAttackRange，威胁度极大
     */
    private Double maxAttackRange;

    /**
     * 杀伤力 / 弹头威力 (0-100)
     * 如果该目标命中保卫区域，造成的伤害值
     * (导弹本体的威力，或者飞机满载弹药的预估总威力)
     */
    private Double damageCapability;



    @Column(length = 1000)
    private String description;
}
