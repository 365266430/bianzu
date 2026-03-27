<template>
  <div></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, inject } from 'vue';
import type {EnemyNode} from '@/model/enemy';
import type { ProtectionZone } from '@/model/protectionZone';
import { type Map, type CircleMarker, circle } from 'leaflet';
import { wsClient } from '@/utils/websocket';
import { createEnemy,createZone } from '@/composables/useMarkers';

const map = inject<Map>('map'); // 确保地图实例已注入
if (!map) {
  throw new Error('Map instance not provided via inject("map")');
}

// 存储当前在地图上的敌方标记，更新时先清除再重建
let enemyMarkers: CircleMarker[] = [];
let zoneMarkers: CircleMarker[] = [];


// 定义一个处理函数
const handleEnemyUpdate = (enemies: EnemyNode[]) => {
  // 更新 Leaflet 地图
  // updateLayer(enemies)...
  console.log("收到敌人数据:", enemies.length);
  updateEnemies(enemies);

};
const handleZoneUpdate = (zones: ProtectionZone[]) => {
  // 更新 Leaflet 地图
  // updateLayer(enemies)...
  console.log("收到保护区数据:", zones.length);
  console.log(zones);
  updateZones(zones);
};

onMounted(() => {
  // 订阅 'ENEMY_UPDATE' 类型的消息
  // 注意：这个 type 字符串要和后端 WebSocketMessage.type 保持一致！
  wsClient.subscribe('ENEMY_UPDATE', handleEnemyUpdate);
  wsClient.subscribe('ZONE_UPDATE', handleZoneUpdate);

  // createEnemy(map, [ 39.915,116.404]);
});

onUnmounted(() => {
  // 清理地图上的敌方标记
  clearEnemyMarkers();
  // 记得取消订阅，防止内存泄漏
  wsClient.unsubscribe('ENEMY_UPDATE', handleEnemyUpdate);
  wsClient.unsubscribe('ZONE_UPDATE', handleZoneUpdate);
});

const updateEnemies = (enemies: EnemyNode[]) => {
  // 先移除已有的敌方 marker，保证地图上只保留最新一批
  clearEnemyMarkers();

  for (const enemy of enemies) {
    const marker = createEnemy(map, [enemy.latitude, enemy.longitude]);
    // 保存以便下次清理
    enemyMarkers.push(marker as CircleMarker);
  }
};

const updateZones = (zones: ProtectionZone[]) => {

  for (const zone of zones) {
    const {marker, circle} = createZone(map, zone.location, 1000000);
    // 保存以便下次清理
    zoneMarkers.push(marker as CircleMarker);
  }
};

function clearEnemyMarkers() {
  try {
    for (const m of enemyMarkers) {
      if (m && map) {
        // map.removeLayer 接受任何 L.Layer（CircleMarker 是 Layer）
        map.removeLayer(m as any);
      }
    }
  } finally {
    enemyMarkers = [];
  }
}

</script>