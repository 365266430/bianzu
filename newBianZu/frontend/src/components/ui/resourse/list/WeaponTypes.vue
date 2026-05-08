<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useResStore } from '@/stores/resource';
import { resApi } from '@/api/resource';
import type { WeaponType } from '@/model/weaponType';

const store = useResStore();
onMounted(() => {
    store.reLoadWeaponTypes();
});

// Map 转 Array 才能遍历
const weaponList = computed(() => Array.from(store.weaponTypeMap.values()));

const emit = defineEmits(['add-weapon', 'edit-weapon']);
function onAddWeapon() {
  console.log('resource/list/WeaponTypes.vue:添加新的武器类型');
  emit('add-weapon');
}

// 编辑 / 删除 按钮处理（当前仅在控制台输出）
function onEditWeapon(type: string) {
  const weapon = store.weaponTypeMap.get(type);
  if (weapon) {
    emit('edit-weapon', weapon);
  }
}
function onDeleteWeapon(type: string) {
      // 询问用户是否确认删除
  const ok = window.confirm(`确定要删除火力类型 “${type}” 吗？此操作不可撤销。`);
  if (!ok) 
    return;

  resApi.deleteWeaponType(type).then((res) => {
    if(res && res.code !==200){
      window.alert(res.message || '删除失败');
      return;
    }
    alert(res.data || '删除成功');
    store.reLoadWeaponTypes();
  }).catch((e) => {
    alert('删除失败: ' + e.message || '');
  });
}
</script>

<template>
  <div class="weapon-list">
    <div v-if="weaponList.length===0" class="empty">暂无武器类型数据</div>
    <div class="grid">
      <article v-for="weapon in weaponList" :key="weapon.type" class="weapon-card">
        <!-- 操作按钮，悬浮时显示 -->
        <div class="card-actions" aria-hidden="true">
          <button class="action-btn edit" @click.stop="onEditWeapon(weapon.type)" title="编辑">✎</button>
          <button class="action-btn del" @click.stop="onDeleteWeapon(weapon.type)" title="删除">×</button>
        </div>

        <div class="card-header">
          <h3>
            {{ weapon.type }}
            <span class="tag">{{ weapon.deployDomain }}单位</span>
          </h3>
        </div>

        <div class="card-body">
          <p><strong>功能:</strong> {{ weapon.function || '-' }}</p>
          <p><strong>火力通道:</strong> {{ weapon.channelCount ?? '-' }}</p>

          <div class="ammo-info" v-if="weapon.fireTypes && weapon.fireTypes.length">
            <h4>配弹方案:</h4>
            <ul>
              <li v-for="(ft, idx) in weapon.fireTypes" :key="idx">
                {{ ft.fireType }} × {{ ft.quantity }} 枚
              </li>
            </ul>
          </div>
        </div>
        <div class="card-info">
          <p><strong>简介:</strong><span class="sub-text">{{ weapon.description || '-' }}</span></p>
        </div>
      </article>

      <!-- 新增占位卡片：用于添加新的武器类型 -->
      <div
        class="weapon-card add-card"
        role="button"
        tabindex="0"
        @click="onAddWeapon"
        @keydown.enter.prevent="onAddWeapon"
        @keydown.space.prevent="onAddWeapon"
        aria-label="添加新的武器类型">
        <div class="add-content">
          <div class="plus">+</div>
          <div class="add-text">添加新的武器类型</div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.weapon-list {
  padding: 12px;
}
.weapon-card {
  position: relative; /* 支持右上角操作按钮定位 */
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
.card-body p { margin: 6px 0; color: #333; }
.sub-text { color: #666; font-size: 0.9em; margin-left: 5px; }
.card-info { color: #013480; }

/* 操作按钮：默认隐藏，仅在 hover 时显示 */
.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.12s ease, transform 0.12s ease;
  pointer-events: none;
}
.weapon-card:hover .card-actions {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}
.action-btn {
  background: rgba(255,255,255,0.95);
  border: 1px solid rgba(0,0,0,0.06);
  padding: 8px 10px;
  min-width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.action-btn.edit { color: #2563eb; }
.action-btn.del { color: #ef4444; }
.action-btn:focus { outline: 2px solid rgba(37,99,235,0.12); }

/* 新增占位卡片样式，居中显示，高度固定 */
.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px; /* 指定高度 */
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
