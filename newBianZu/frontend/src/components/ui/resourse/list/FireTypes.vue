<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useResStore } from '@/stores/resource';
import { resApi } from '@/api/resource';

const store = useResStore();

onMounted(() => {
    store.reLoadFireTypes();
});
// Map 转 Array 以便遍历
const fireTypeList = computed(() => Array.from(store.fireTypeMap?.values?.() || []));

const emit = defineEmits(['add-firetype']);
function onAddFireType() {
  console.log('resource/list/FireTypes.vue:添加新的火力类型');
  emit('add-firetype');
}

function onEditFireType(type: any) {
  // console.log('编辑火力类型:', ft?.type ?? ft);
}
function onDeleteFireType(type: string) {
    // 询问用户是否确认删除
  const ok = window.confirm(`确定要删除火力类型 “${type}” 吗？此操作不可撤销。`);
  if (!ok) 
    return;

  resApi.deleteFireType(type).then((res) => {
    if(res && res.code !==200){
      window.alert(res.message || '删除失败');
      return;
    }
    alert(res.data || '删除成功');
    // 删除后刷新资源字典（若 store 提供该方法）
    store.reLoadFireTypes();

}).catch((e) => {
    alert('删除失败: ' + e.message || '');
  });
}
</script>

<template>
  <div class="fire-list">
    <div v-if="fireTypeList.length===0" class="empty">暂无火力单元类型数据</div>
    <div class="grid">
      <article v-for="ft in fireTypeList" :key="ft.type" class="fire-card">
        <div class="card-actions" aria-hidden="true">
          <button class="action-btn edit" @click.stop="onEditFireType(ft.type)" title="编辑">✎</button>
          <button class="action-btn del" @click.stop="onDeleteFireType(ft.type)" title="删除">×</button>
        </div>

        <div class="card-header">
          <h3>
            {{ ft.type }}
            <span class="tag">火力单元</span>
          </h3>
        </div>

        <div class="card-body">
          <p><strong>成本:</strong> {{ ft.cost ?? '-' }}</p>
          <p><strong>拦截概率:</strong> {{ ft.interception ?? '-' }}</p>
          <p><strong>最大射程:</strong> {{ ft.maxRange ?? '-' }} m</p>
          <p><strong>最小射程:</strong> {{ ft.minRange ?? '-' }} m</p>
          <p><strong>最大射高:</strong> {{ ft.maxAlt ?? '-' }} m</p>
          <p><strong>最小射高:</strong> {{ ft.minAlt ?? '-' }} m</p>
          <p><strong>调度成本:</strong> {{ ft.attCost ?? '-' }}</p>
        </div>
        <div class="card-info">
          <p><strong>简介:</strong><span class="sub-text">{{ ft.description || '-' }}</span></p>
        </div>
      </article>

      <!-- 新增占位卡片：用于添加新的火力类型 -->
      <div
        class="fire-card add-card"
        role="button"
        tabindex="0"
        @click="onAddFireType"
        @keydown.enter.prevent="onAddFireType"
        @keydown.space.prevent="onAddFireType"
        aria-label="添加新的火力类型">
        <div class="add-content">
          <div class="plus">+</div>
          <div class="add-text">添加新的火力类型</div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.fire-list { padding: 12px; }
.empty { color: #6b7280; padding: 24px; text-align: center; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.fire-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  background: white;
  position: relative;
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

/* 新增占位卡片样式 */
.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  /* height: 200px;  */
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
.fire-card:hover .card-actions {
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
</style>