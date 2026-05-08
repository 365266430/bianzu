<template>
  <div class="add-fire-wrap">
    <section class="card">
      <header class="card-head">
        <h3>添加火力类型</h3>
        <p class="subtitle">填写火力单元的性能参数</p>
      </header>

      <div class="card-body form-grid">
        <label class="field">
          <span class="label">火力类型名称</span>
          <input v-model="form.type" type="text" :disabled="isEditMode" />
        </label>

        <label class="field">
          <span class="label">成本</span>
          <input v-model.number="form.cost" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">拦截概率</span>
          <input v-model.number="form.interception" type="number" step="0.01" min="0" max="1" />
        </label>

        <label class="field">
          <span class="label">最大射程（m）</span>
          <input v-model.number="form.maxRange" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">最小射程（m）</span>
          <input v-model.number="form.minRange" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">最大射高（m）</span>
          <input v-model.number="form.maxAlt" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">最小射高（m）</span>
          <input v-model.number="form.minAlt" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">调度成本</span>
          <input v-model.number="form.attCost" type="number" min="0" />
        </label>

        <label class="field full">
          <span class="label">描述</span>
          <textarea v-model="form.description" rows="3"></textarea>
        </label>

        <p v-if="message" class="msg success">{{ message }}</p>
        <p v-if="error" class="msg error">{{ error }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { FireType } from '@/model/fireType';

const message = ref('');
const error = ref('');
const loading = ref(false);
import { resApi } from '@/api/resource';

const props = defineProps<{
  initialValue?: FireType | null;
  mode?: 'add' | 'edit';
}>();

const isEditMode = computed(() => props.mode === 'edit');

const form = ref<FireType>({
  type: '',
  cost: 0,
  interception: 0,
  maxRange: 0,
  minRange: 0,
  maxAlt: 0,
  minAlt: 0,
  attCost: 0,
  description: ''
});

function defaultForm(): FireType {
  return {
    type: '',
    cost: 0,
    interception: 0,
    maxRange: 0,
    minRange: 0,
    maxAlt: 0,
    minAlt: 0,
    attCost: 0,
    description: ''
  };
}

watch(
  () => props.initialValue,
  (value) => {
    form.value = value ? { ...value } : defaultForm();
    resetMessage();
  },
  { immediate: true }
);

function resetMessage() {
  message.value = '';
  error.value = '';
}

async function submitForm() {
  resetMessage();
  loading.value = true;
  // 简单校验
  if (!form.value.type) {
    error.value = '请填写类型名称';
    return false;
  }
  try {
    const payload = { ...form.value } as FireType;
    const res = await resApi.addFireType(payload);
    if(res && res.code == 200){
      message.value = res.message || (isEditMode.value ? '保存成功' : '添加成功');
      return true;
    } else {
        error.value = res.message || '保存失败';
        throw new Error(res.message || '保存失败');
    }
  } catch (err: any) {
    console.error('添加失败', err);
    error.value = err?.message || String(err);
    return false;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = props.initialValue ? { ...props.initialValue } : defaultForm();
  resetMessage();
}
// 将方法暴露给父组件调用
defineExpose({ submitForm, resetForm });
</script>

<style scoped>
.add-fire-wrap { display:flex; justify-content:center; padding: 18px; }
.card { width: 100%; max-width: 720px; background: #fff; border-radius: 10px; box-shadow: 0 8px 24px rgba(16,24,40,0.06); overflow: hidden; }
.card-head { padding: 18px 20px; border-bottom: 1px solid #f1f5f9; font-size: 20px; }
.card-head h3 { margin: 0; font-size: 20px; }
.subtitle { margin: 6px 0 0 0; color: #6b7280; font-size: 14px; }
.card-body { padding: 18px 20px; font-size: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display:flex; flex-direction: column; }
.field.full { grid-column: 1 / -1; }
.label { font-size: 16px; color: #374151; margin-bottom: 6px; }
input[type='text'], input[type='number'] { padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 18px; color: #0f1724; }
textarea { padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 16px; color: #191e25; resize: vertical; }
.msg { grid-column: 1 / -1; margin-top: 8px; }
.msg.success { color: #16a34a; }
.msg.error { color: #dc2626; }

@media (max-width: 680px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
