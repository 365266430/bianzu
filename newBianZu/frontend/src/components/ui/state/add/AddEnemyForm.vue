<script setup lang="ts">
import { ref, computed } from 'vue';
import { useResStore } from '@/stores/resource';
import CompassInput from "../../common/CompassInput.vue";

// 接收父组件传来的坐标
const props = defineProps<{
  lat: number;
  lng: number;
}>();

const emit = defineEmits(['submit', 'cancel']);
const store = useResStore();

// 表单数据模型，添加 speed 字段
const form = ref({
  type: '', // 必填，从字典选
  heading: 90,
  altitude: 10000,
  count: 1, // 批量添加功能 (一次生成编队)
  speed: 0
});

// 从 Store 获取下拉选项
const enemyTypes = computed(() => Array.from(store.enemyTypeMap.values()));

const handleSubmit = () => {
  if (!form.value.type) {
    alert("请选择目标类型");
    return;
  }
  // 把表单数据 + 坐标 一起发出去
  emit('submit', { ...form.value, lat: props.lat, lng: props.lng });
};
</script>

<template>
  <div class="form-container two-column">
    <div class="left-col">
      <div class="form-item">
        <label>坐标:</label>
        <span>{{ lat.toFixed(4) }}, {{ lng.toFixed(4) }}</span>
      </div>

      <div class="form-item">
        <label>目标类型:</label>
        <select v-model="form.type">
          <option disabled value="">请选择</option>
          <option v-for="t in enemyTypes" :key="t.type" :value="t.type">
            {{ t.type }} ({{ t.category }})
          </option>
        </select>
      </div>

      <div class="form-item">
        <label>速度（马赫）:</label>
        <input type="number" v-model.number="form.speed" min="0" />
      </div>
    </div>

    <div class="right-col">
      <div class="form-item">
        <label>航向设定:</label>
        <!-- 使用罗盘组件 -->
        <CompassInput v-model:compassAngle="form.heading" />
      </div>
    </div>

    <div class="form-actions full">
      <button @click="$emit('cancel')">取消</button>
      <button class="primary" @click="handleSubmit">生成</button>
    </div>
  </div>
</template>

<style scoped>
.form-container { display:flex; flex-direction:column; gap:12px; }
.two-column { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; align-items: start; }
.left-col, .right-col { display:flex; flex-direction:column; gap:12px; }
.form-item { margin-bottom: 0; }
.form-item label { display: block; font-weight: bold; margin-bottom: 6px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.form-actions.full { grid-column: 1 / -1; }
button.primary { background-color: #2196F3; color: white; border: none; padding: 8px 12px; border-radius: 6px; }
input[type='number'], select { padding: 6px 8px; border: 1px solid #e5e7eb; border-radius: 6px; }

@media (max-width: 720px) {
  .two-column { grid-template-columns: 1fr; }
  .form-actions.full { justify-content: stretch; }
}
</style>