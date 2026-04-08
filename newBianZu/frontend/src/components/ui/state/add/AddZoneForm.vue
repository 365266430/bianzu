<script setup lang="ts">
import { ref, watch } from 'vue'

interface ZoneFormValue {
  name: string
  size: number
  value: 1 | 2 | 3
  health: number
}

const props = defineProps<{
  lat: number
  lng: number
  initialValue?: Partial<ZoneFormValue> | null
  submitLabel?: string
}>()

const emit = defineEmits(['submit', 'cancel'])

const form = ref<ZoneFormValue>({
  name: '',
  size: 25000,
  value: 2,
  health: 10,
})

watch(
  () => props.initialValue,
  value => {
    form.value = {
      name: value?.name ?? '',
      size: value?.size ?? 25000,
      value: (value?.value ?? 2) as 1 | 2 | 3,
      health: value?.health ?? 10,
    }
  },
  { immediate: true },
)

const handleSubmit = () => {
  emit('submit', {
    ...form.value,
    lat: props.lat,
    lng: props.lng,
  })
}
</script>

<template>
  <div class="form-container">
    <div class="form-item">
      <label>坐标:</label>
      <span>{{ lat.toFixed(4) }}, {{ lng.toFixed(4) }}</span>
    </div>

    <div class="form-item">
      <label>名称:</label>
      <input v-model.trim="form.name" type="text" placeholder="例如 Zone-East" />
    </div>

    <div class="form-item">
      <label>保护区半径(米):</label>
      <input v-model.number="form.size" type="number" min="1" />
    </div>

    <div class="form-item">
      <label>价值等级:</label>
      <select v-model.number="form.value">
        <option :value="1">1</option>
        <option :value="2">2</option>
        <option :value="3">3</option>
      </select>
    </div>

    <div class="form-item">
      <label>健康度:</label>
      <input v-model.number="form.health" type="number" min="1" max="10" />
    </div>

    <div class="form-actions">
      <button @click="$emit('cancel')">取消</button>
      <button class="primary" @click="handleSubmit">{{ submitLabel || '生成' }}</button>
    </div>
  </div>
</template>

<style scoped>
.form-container { display: flex; flex-direction: column; gap: 12px; }
.form-item label { display: block; font-weight: bold; margin-bottom: 6px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
button.primary { background-color: #2196F3; color: white; border: none; padding: 8px 12px; border-radius: 6px; }
input[type='text'], input[type='number'], select { padding: 6px 8px; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
