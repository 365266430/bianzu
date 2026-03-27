package com.bianzu.bianzu_backend.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 敌方实体包装类 (包含位置信息)
 * 对应前端: Enemy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnemyNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符
     * 例如: "E-001"
     */
    private String id;

    /**
     * 识别出的目标类型 (关联 EnemyType)
     * 例如: "F-35", "Tomahawk"
     */
    private String type;

    /**
     * 经度 (Longitude)
     */
    private Double longitude;

    /**
     * 纬度 (Latitude)
     */
    private Double latitude;

    /**
     * 飞行高度 (Altitude, 单位: 米)
     * 影响雷达探测和拦截弹包线
     */
    private Double altitude;

    /**
     * 航向角 (Heading, 单位: 度, 0-360)
     * 0为正北，90为正东
     * 用于计算威胁意图 (是否飞向保卫目标)
     */
    private Double heading;

    /**
     * 当前实时速度 (单位: 马赫)
     * 初始生成时可以设为 EnemyType.maxSpeed 的 80%
     */
    private Double speed;

}
