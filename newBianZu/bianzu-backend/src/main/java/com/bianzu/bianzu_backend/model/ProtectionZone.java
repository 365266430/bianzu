package com.bianzu.bianzu_backend.model;


import com.bianzu.bianzu_backend.config.DoubleListConverter;
import com.bianzu.bianzu_backend.config.StringListConverter;
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
 * 保护区实体类
 * 对应前端: ProtectionZone
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "protection_zones")
public class ProtectionZone implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 保护区编号或名称
     * 前端: ID (string | number)
     */
    @Id
    @Column(length = 100)
    private String id;

    /**
     * 保护区的坐标 [x, y]
     * 前端: Location: [number, number]
     * Java中使用 List<Double> (或 Double[]) 存储，例如 [120.5, 30.0]
     */
    @Lob
    @Convert(converter = DoubleListConverter.class)
    private List<Double> location;

    /**
     * 保护区的大小（半径或长宽）
     * 前端: Size
     */
    private Double size;

    /**
     * 保护区目标的价值（1、2、3）
     * 前端: Value
     */
    private Integer value;

    /**
     * 保护区的健康情况(1-10)
     * 前端: Heath (注意前端拼写为 Heath，这里注解保持一致)
     */
    private Integer health;

    /*
    * 保护区关联的武器装备ID
    * */
    @Lob
    @Convert(converter = StringListConverter.class)
    private List<String> stationedWeaponIds;
}
