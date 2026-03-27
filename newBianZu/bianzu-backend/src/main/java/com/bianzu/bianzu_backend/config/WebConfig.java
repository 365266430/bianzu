package com.bianzu.bianzu_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 全局配置
 * 主要用于配置 CORS (跨域资源共享)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求路径
        registry.addMapping("/**")
                // 允许所有来源 (Vue 前端地址，生产环境建议写具体域名)
                // Spring Boot 2.4+ 建议使用 allowedOriginPatterns
                .allowedOriginPatterns("*")
                // 允许的方法 (GET, POST, etc)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的 Header
                .allowedHeaders("*")
                // 是否允许携带 Cookie (如果允许，allowedOrigins 不能为 *)
                .allowCredentials(true)
                // 预检请求(OPTIONS)缓存时间 (秒)
                .maxAge(3600);
    }
}
