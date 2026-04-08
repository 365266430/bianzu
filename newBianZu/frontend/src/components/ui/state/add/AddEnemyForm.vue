<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useResStore } from '@/stores/resource'
import CompassInput from '../../common/CompassInput.vue'

interface EnemyFormValue {
  name: string
  type: string
  heading: number
  altitude: number
  count: number
  speed: number
}

const props = defineProps<{
  lat: number
  lng: number
  initialValue?: Partial<EnemyFormValue> | null
  submitLabel?: string
}>()

const emit = defineEmits(['submit', 'cancel'])
const store = useResStore()

const form = ref<EnemyFormValue>({
  name: '',
  type: '',
  heading: 90,
  altitude: 10000,
  count: 1,
  speed: 0,
})

const enemyTypes = computed(() => Array.from(store.enemyTypeMap.values()))

watch(
  () => props.initialValue,
  value => {
    form.value = {
      name: value?.name ?? '',
      type: value?.type ?? '',
      heading: value?.heading ?? 90,
      altitude: value?.altitude ?? 10000,
      count: value?.count ?? 1,
      speed: value?.speed ?? 0,
    }
  },
  { immediate: true },
)

const handleSubmit = () => {
  if (!form.value.type) {
    alert('请选择目标类型')
    return
  }

  emit('submit', { ...form.value, lat: props.lat, lng: props.lng })
}
</script>

<template>
  <div class="form-container two-column">
    <div class="left-col">
      <div class="form-item">
        <label>坐标:</label>
        <span>{{ lat.toFixed(4) }}, {{ lng.toFixed(4) }}</span>
      </div>

      <div class="form-item">
        <label>名称:</label>
        <input v-model.trim="form.name" type="text" placeholder="例如 E-Alpha-01" />
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
        <label>速度(马赫):</label>
        <input type="number" v-model.number="form.speed" min="0" />
      </div>
    </div>

    <div class="right-col">
      <div class="form-item">
        <label>航向设定:</label>
        <CompassInput v-model:compassAngle="form.heading" />
      </div>
    </div>

    <div class="form-actions full">
      <button @click="$emit('cancel')">取消</button>
      <button class="primary" @click="handleSubmit">{{ submitLabel || '生成' }}</button>
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
input[type='text'], input[type='number'], select { padding: 6px 8px; border: 1px solid #e5e7eb; border-radius: 6px; }

@media (max-width: 720px) {
  .two-column { grid-template-columns: 1fr; }
  .form-actions.full { justify-content: stretch; }
}
</style>
