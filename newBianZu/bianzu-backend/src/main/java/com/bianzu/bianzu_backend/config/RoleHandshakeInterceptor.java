package com.bianzu.bianzu_backend.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class RoleHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            //转换为 Servlet 请求
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 1. 获取请求中的参数 (例如: ws://.../ws/sim?role=monitor)
            String role = servletRequest.getServletRequest().getParameter("role");

            // 2. 如果参数存在，把它存入 attributes
            // 注意：这里的 attributes 最终会变成 WebSocketSession.getAttributes()
            if (role != null) {
                attributes.put("SESSION_ROLE", role);
            } else {
                // 如果没传角色，可以设置一个默认值，或者拒绝连接(return false)
                attributes.put("SESSION_ROLE", "DEFAULT");
            }
        }
        return true; // 返回 true 表示同意握手
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
