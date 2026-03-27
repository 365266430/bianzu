package com.bianzu.bianzu_backend.config;

import com.bianzu.bianzu_backend.handler.SimulationHandler;
import com.bianzu.bianzu_backend.service.SimulationEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SimulationHandler simulationHandler;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 定义 ws 路径，并允许跨域
        registry.addHandler(simulationHandler, "/ws/sim")
                .setAllowedOrigins("*").addInterceptors(new RoleHandshakeInterceptor());
    }
}