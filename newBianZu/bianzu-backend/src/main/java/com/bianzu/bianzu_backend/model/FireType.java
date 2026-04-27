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
 * 火力单元实体类
 */
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@AllArgsConstructor // 自动生成全参构造函数
@NoArgsConstructor  // 自动生成无参构造函数
@Entity
@Table(name = "fire_types")
public class FireType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 火力单元类型名称 or 编号
     */
    @Id
    @Column(length = 100)
    private String type;

    /**
     * 火力单元成本指数（万）
     */
    private Double cost;

    /**
     * 拦截概率 (0.0 - 1.0)
     */
    private Double interception;

    /**
     * 最大动力射程（米）
     */
    private Double maxRange;

    /**
     * 最小动力射程（米）
     */
    private Double minRange;

    /**
     * 最大射高（米）
     */
    private Double maxAlt;

    /**
     * 最小射高（米）
     */
    private Double minAlt;

    /**
     * 调度成本，表示每公里/万元
     */
    private Double attCost;

    @Column(length = 1000)
    private String description;
}
