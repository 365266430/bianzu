<template>
  <div class="add-enemy-wrap">
    <section class="card">
      <header class="card-head">
        <h3>添加敌方类型</h3>
        <p class="subtitle">填写并提交新敌方类型</p>
      </header>

      <form class="card-body form-grid">
        <label class="field">
          <span class="label">类型名称</span>
          <input v-model="form.type" type="text" required :disabled="isEditMode" />
        </label>

        <label class="field">
          <span class="label">类别</span>
          <select v-model="form.category" required>
            <option v-for="opt in ENEMY_CATEGORY_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </label>

        <label class="field">
          <span class="label">价值（万元）</span>
          <input v-model.number="form.value" type="number" min="0" required />
        </label>

        <label class="field">
          <span class="label">最大速度（马赫）</span>
          <input v-model.number="form.maxSpeed" type="number" step="0.1" min="0" required />
        </label>

        <label class="field">
          <span class="label">典型巡航高度（m）</span>
          <input v-model.number="form.typicalAltitude" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">杀伤力（0-100）</span>
          <input v-model.number="form.damageCapability" type="number" min="0" max="100" />
        </label>

        <label class="field">
          <span class="label">最大打击半径（m）</span>
          <input v-model.number="form.maxAttackRange" type="number" min="0" />
        </label>

        <label class="field">
          <span class="label">RCS（m²）</span>
          <input v-model.number="form.rcs" type="number" step="0.001" min="0" required />
        </label>


        <label class="field full">
          <span class="label">描述</span>
          <textarea v-model="form.description" rows="3"></textarea>
        </label>

        <p v-if="message" class="msg success">{{ message }}</p>
        <p v-if="error" class="msg error">{{ error }}</p>
      </form>

    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { EnemyType } from '@/model/enemyType';
import { resApi } from '@/api/resource';
import { useResStore } from '@/stores/resource';
import { ENEMY_CATEGORY_OPTIONS } from '@/model/enemyCategory';

const loading = ref(false);
const message = ref('');
const error = ref('');

const store = useResStore();

const props = defineProps<{
  initialValue?: EnemyType | null;
  mode?: 'add' | 'edit';
}>();

const isEditMode = computed(() => props.mode === 'edit');

const form = ref<EnemyType>({
  type: '',
  category: ENEMY_CATEGORY_OPTIONS[0]?.value || '',
  value: 0,
  maxSpeed: 0,
  typicalAltitude: 0,
  rcs: 0,
  maxAttackRange: 0,
  damageCapability: 0,
  description: '这是一个敌方类型'
});

function defaultForm(): EnemyType {
  return {
    type: '',
    category: ENEMY_CATEGORY_OPTIONS[0]?.value || '',
    value: 0,
    maxSpeed: 0,
    typicalAltitude: 0,
    rcs: 0,
    maxAttackRange: 0,
    damageCapability: 0,
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
    const payload = { ...form.value } as EnemyType;
    const res = await resApi.addEnemyType(payload);
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

defineExpose({
  submitForm,
  resetForm,
});
</script>

<style scoped>
.add-enemy-wrap { display:flex; justify-content:center; padding: 18px; }
.card { width: 100%; max-width: 720px; background: #fff; border-radius: 10px; box-shadow: 0 8px 24px rgba(16,24,40,0.06); overflow: hidden; }
.card-head { padding: 18px 20px; border-bottom: 1px solid #f1f5f9; }
.card-head h3 { margin: 0; font-size: 20px; color: #0f1724; }
.subtitle { margin: 6px 0 0 0; color: #6b7280; font-size: 16px; }
.card-body { padding: 18px 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display:flex; flex-direction: column; }
.field.full { grid-column: 1 / -1; }
.field-inline { flex-direction: row; align-items: center; gap: 8px; }
.label { font-size: 18px; color: #374151; margin-bottom: 6px; }
input[type='text'], input[type='number'],select{ padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 18px; color: #0f1724; }
textarea{padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 16px; color: #191e25; }
textarea { resize: vertical; }
.actions { display:flex; gap: 10px; justify-content: flex-end; }
.btn { padding: 8px 14px; border-radius: 8px; border: none; cursor: pointer; font-weight: 600; }
.btn.primary { background: #2563eb; color: #fff; }
.btn.ghost { background: transparent; border: 1px solid #e6e9ef; color: #374151; }
.msg { grid-column: 1 / -1; margin-top: 8px; }
.msg.success { color: #16a34a; }
.msg.error { color: #dc2626; }

/* Responsive: single-column on small screens */
@media (max-width: 680px) {
  .form-grid { grid-template-columns: 1fr; }
  .actions { justify-content: stretch; }
}
</style>
