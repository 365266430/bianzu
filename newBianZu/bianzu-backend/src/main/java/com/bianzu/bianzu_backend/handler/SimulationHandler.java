package com.bianzu.bianzu_backend.handler;

import com.alibaba.fastjson2.JSON;
import com.bianzu.bianzu_backend.common.WebSocketMessage;
import com.bianzu.bianzu_backend.model.EnemyNode;
import com.bianzu.bianzu_backend.model.ProtectionZone;
import com.bianzu.bianzu_backend.model.WeaponNode;
import com.bianzu.bianzu_backend.service.EnemyNodeService;
import com.bianzu.bianzu_backend.service.ProtectionZoneService;
import com.bianzu.bianzu_backend.service.WeaponNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SimulationHandler extends TextWebSocketHandler {
    // 线程安全的 Session 集合，存所有连接的前端用户
    public final CopyOnWriteArrayList<WebSocketSession> SESSIONS = new CopyOnWriteArrayList<>();

    // 2. 直接注入数据层 Service
    @Autowired
    private EnemyNodeService enemyService;

    @Autowired
    private ProtectionZoneService zoneService;

    @Autowired
    private WeaponNodeService weaponNodeService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSIONS.add(session); // 有人连进来了，记下来
        System.out.println("前端已连接: " + session.getId());
        sendInitialData(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.remove(session); // 人走了，删掉
    }

    public void pushToFrontend(String msg){
        for (WebSocketSession session : SESSIONS) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(msg));
                } catch (IOException e) {
                    System.err.println("推送失败: " + e.getMessage());
                }
            }
        }
    }
    // 私有辅助方法
    private void sendInitialData(WebSocketSession session) {
        try {
            List<EnemyNode> enemies = enemyService.getAllEnemies();
            List<ProtectionZone> zones = zoneService.getAllZones();
            List<WeaponNode> weapons = weaponNodeService.getAllWeapons();
            if (enemies!=null&&!enemies.isEmpty()) {
                WebSocketMessage<List<EnemyNode>> msg = WebSocketMessage.of("ENEMY_UPDATE", enemies);
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            }

            if (zones!=null&&!zones.isEmpty()) {
                WebSocketMessage<List<ProtectionZone>> msg = WebSocketMessage.of("ZONE_UPDATE", zones);
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            }

            if (weapons != null && !weapons.isEmpty()) {
                WebSocketMessage<List<WeaponNode>> msg = WebSocketMessage.of("WEAPON_UPDATE", weapons);
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
