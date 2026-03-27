package com.bianzu.bianzu_backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage<T> {
    // 消息类型：前端根据这个字段 switch-case
    // 例如: "ENEMY_UPDATE", "ZONE_UPDATE", "GAME_OVER", "GLOBAL_ALERT"
    private String type;

    // 真实数据
    private T payload;

    // 时间戳 (System.currentTimeMillis())
    private long timestamp;

    // 静态辅助方法：快速创建消息
    public static <T> WebSocketMessage<T> of(String type, T payload) {
        return new WebSocketMessage<>(type, payload, System.currentTimeMillis());
    }
}
