package com.bianzu.bianzu_backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bianzu.bianzu_backend.model.EnemyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敌方类型服务
 * 职责：管理静态的武器属性库 (Library/Dictionary)
 */
@Service
public class EnemyTypeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis Key: 使用 Hash 结构存储，Key=类型名称, Value=EnemyUnit对象
    private static final String KEY_ENEMY_TYPES = "sim:config:enemy_types";

    /**
     * 根据名称获取类型模板
     * @param typeName 例如 "F-35"
     */
    public EnemyType getEnemyType(String typeName) {
        // 从 Redis Hash 中取出一个对象
        Object obj = redisTemplate.opsForHash().get(KEY_ENEMY_TYPES, typeName);
        if(obj==null){
            System.out.println("redis中敌方类型不存在{EnemyTypeService}");
            return null;
        }
        JSONObject jsonObject=(JSONObject)obj;
        EnemyType enemyUnit=jsonObject.toJavaObject(EnemyType.class);
        return enemyUnit;
    }

    public List<EnemyType> getEnemyTypes(){
        List<Object> objList = redisTemplate.opsForHash().values(KEY_ENEMY_TYPES);
        // 2. 空值处理
        if (objList == null||objList.isEmpty()) {
            return new ArrayList<>();
        }

        List<EnemyType> result = new ArrayList<>();

        // 3. 遍历并转换
        for (Object obj : objList) {
            if (obj == null) continue;

            if (obj instanceof EnemyType) {
                // 如果已经是实体类，直接强转
                result.add((EnemyType) obj);
            } else if (obj instanceof JSONObject) {
                // 如果是 FastJSON 对象，转为实体类
                result.add(((JSONObject) obj).toJavaObject(EnemyType.class));
            } else {
                // 兜底：尝试先转 JSON 字符串再解析 (防止其他序列化情况)
                try {
                    String json = JSON.toJSONString(obj);
                    result.add(JSON.parseObject(json, EnemyType.class));
                } catch (Exception e) {
                    System.err.println("解析 EnemyType 失败: " + obj);
                }
            }
        }

        return result;
    }

    /**
     * 新增或更新敌方单位类型
     * @param enemyType 前端传来的对象
     */
    public void addEnemyType(EnemyType enemyType) {
        // 1. 简单校验
        if (enemyType == null || enemyType.getType() == null || enemyType.getType().isEmpty()) {
            throw new IllegalArgumentException("敌方类型名称(type)不能为空");
        }
        // 2. 存入 Redis
        // Key: sim:config:enemy_types
        // HashKey: enemyType.type (例如 "F-22")
        // Value: enemyType 对象 (自动序列化为 JSON)
        redisTemplate.opsForHash().put(KEY_ENEMY_TYPES, enemyType.getType(), enemyType);

        System.out.println(">>> 已添加/更新敌方类型: " + enemyType.getType());
    }


    public void deleteEnemyType(String enemyType){
        redisTemplate.opsForHash().delete(KEY_ENEMY_TYPES,enemyType);
    }
}