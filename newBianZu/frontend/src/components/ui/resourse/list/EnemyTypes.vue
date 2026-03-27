<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useResStore } from '@/stores/resource';
import { resApi } from '@/api/resource';
import { EnemyCategoryLabel } from '@/model/enemyCategory';

const store = useResStore();
  onMounted(() => {
      store.reLoadEnemyTypes();
  });



// Map 转 Array 才能遍历
const enemyList = computed(() => Array.from(store.enemyTypeMap.values()));

const emit = defineEmits(['add-enemy']);

function onAddEnemyType() {
  // 占位：点击添加新的敌方类型，具体逻辑后续实现
  console.log('resource/list/EnemyTypes.vue:添加新的敌方类型');
  emit('add-enemy');
}

// 编辑 / 删除 按钮处理（当前仅在控制台输出）
function onEditEnemy(type: string) {
  console.log('编辑敌方类型:', type);
}
function onDeleteEnemy(type: string) {
  // 询问用户是否确认删除
  const ok = window.confirm(`确定要删除敌方类型 “${type}” 吗？此操作不可撤销。`);
  if (!ok) 
    return;
  resApi.deleteEnemyType(type).then((res) => {
    if (res && res.code !== 200) {
      window.alert(res.message || '删除失败');
      return;
    }
    alert(res.data || '删除成功');
    store.reLoadEnemyTypes();
  }).catch((e) => {
    alert('删除失败:'+e.message || '');
  });
}

function getCategoryLabel(cat: string | undefined) {
  if (!cat) return '';
  return (EnemyCategoryLabel as any)[cat] || cat;
}
</script>

<template>
  <div class="enemy-list">
    <div class="grid">
      <div v-for="enemy in enemyList" :key="enemy.type" class="enemy-card">
        <!-- 操作按钮，悬浮时显示 -->
        <div class="card-actions" aria-hidden="true">
          <button class="action-btn edit" @click.stop="onEditEnemy(enemy.type)" title="编辑">✎</button>
          <button class="action-btn del" @click.stop="onDeleteEnemy(enemy.type)" title="删除">×</button>
        </div>

        <div class="card-header">
          <h3>
            {{ enemy.type }}
            <span class="tag">{{ getCategoryLabel(enemy.category) }}</span>
          </h3>
        </div>

        <div class="card-body">
          <div class="stats-1">
            <p><strong>价值:</strong> {{ enemy.value }}</p>
            <p><strong>最大速度:</strong> {{ enemy.maxSpeed }} m/s</p>
            <p><strong>典型巡航高度:</strong> {{ enemy.typicalAltitude }} m</p>
          </div>
        <div class="stats-2">
            <p><strong>最大打击半径:</strong> {{ enemy.maxAttackRange }} m</p>
            <p><strong>杀伤力:</strong> {{ enemy.damageCapability }}</p>
            <p><strong>RCS:</strong> {{ enemy.rcs }} m²</p>
        </div>
        </div>
        <div class="card-info">
          <p><strong>简介:</strong><span class="sub-text">{{ enemy.description || '-' }}</span></p>
        </div>  
      </div>

      <!-- 新增的占位卡片：用于添加新的敌方类型 -->
      <div
        class="enemy-card add-card"
        role="button"
        tabindex="0"
        @click="onAddEnemyType"
        @keydown.enter.prevent="onAddEnemyType"
        @keydown.space.prevent="onAddEnemyType"
        aria-label="添加新的敌方类型">
        <div class="add-content">
          <div class="plus">+</div>
          <div class="add-text">添加新的敌方类型</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.enemy-list {
  padding: 12px;
}
/* 使用网格排列，每个卡片宽度与现有样式相匹配 */
.grid {
  display: grid;
  gap: 12px;
}
.enemy-card {
  position: relative; /* 为绝对定位的操作按钮提供定位上下文 */
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  background: white;
}
.card-header h3 { margin: 0 0 6px 0; font-size: 16px; display:flex; align-items:center; gap:8px; }
.tag {
  background: #eef2ff;
  color: #2563eb;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  margin-left: 8px;
}
.card-body {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.card-body p { margin: 6px 0; color: #333; width: 100%;}
.stats-1, .stats-2 { flex: 1; }
.sub-text { color: #666; font-size: 0.9em; margin-left: 5px; }
.card-info {color: #013480; }

/* 操作按钮：默认隐藏，仅在 hover 时显示 */
.card-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.12s ease, transform 0.12s ease;
  pointer-events: none;
}
.enemy-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}
.action-btn {
  background: rgba(255,255,255,0.95);
  border: 1px solid rgba(0,0,0,0.06);
  padding: 8px 10px;      /* 更大的可点击区域 */
  min-width: 36px;
  height: 36px;
  border-radius: 8px;     /* 更圆润 */
  cursor: pointer;
  font-size: 14px;        /* 更大图标显示 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.action-btn.edit { color: #2563eb; }
.action-btn.del { color: #ef4444; }
.action-btn:focus { outline: 2px solid rgba(37,99,235,0.12); }

/* 新增卡片样式 */
.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px;
  cursor: pointer;
  border: 2px dashed #e6e6e6;
  background: #fafafa;
  color: #9ca3af; /* 淡灰色字体 */
}
.add-card:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.12);
}
.add-content { text-align: center; }
.plus {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 6px;
}
.add-text {
  font-size: 14px;
  color: #9ca3af;
}
</style>