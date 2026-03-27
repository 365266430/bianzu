package com.bianzu.bianzu_backend.service;


import com.alibaba.fastjson2.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.bianzu.bianzu_backend.model.EnemyType;
import com.bianzu.bianzu_backend.model.EnemyNode;

import java.util.ArrayList;
import java.util.List;
@Service
public class EnemyNodeService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private EnemyTypeService enemyTypeService;

    // Redis Key: 战场实时数据
    private static final String STATE_RED_ENEMIES = "sim:state:red_enemies";


    public List<EnemyNode> getAllEnemies(){
        try{
            Object obj=redisTemplate.opsForValue().get(STATE_RED_ENEMIES);
            if(obj==null) return null;
            List<EnemyNode> objList=((JSONArray)obj).toJavaList(EnemyNode.class);
            return objList;
        }catch (Exception e){
            System.out.println("错误："+e.getMessage());
            return null;
        }
    }
    /**
     * 保存所有敌方节点到 Redis
     * 操作：全量覆盖 (Overwrite)
     * @param enemies 最新的敌方列表
     */
    public void saveAllEnemies(List<EnemyNode> enemies) {
        if (enemies == null) {
            return;
        }
        // 直接存 List，RedisTemplate 会自动调用 FastJSON 转成 JSON 数组字符串
        // Key: sim:state:red_nodes
        redisTemplate.opsForValue().set(STATE_RED_ENEMIES, enemies);
    }
}
