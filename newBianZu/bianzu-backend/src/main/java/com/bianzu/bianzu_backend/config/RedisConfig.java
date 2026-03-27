package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.support.spring6.data.redis.FastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. JSON 序列化工具 (使用 FastJSON2)
        FastJsonRedisSerializer<Object> fastJsonSerializer = new FastJsonRedisSerializer<>(Object.class);
        // String 序列化工具
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 2. key 的序列化 (String)
        template.setKeySerializer(stringSerializer);
        // 3. value 的序列化 (JSON)
        template.setValueSerializer(fastJsonSerializer);

        // ============ 关键点在这里 ============
        // 4. Hash Key 的序列化 (String)
        template.setHashKeySerializer(stringSerializer);
        // 5. Hash Value 的序列化 (JSON) —— 这一行决定了 Hash 里的内容是不是乱码
        template.setHashValueSerializer(fastJsonSerializer);
        // ===================================

        template.afterPropertiesSet();
        return template;
    }
}
