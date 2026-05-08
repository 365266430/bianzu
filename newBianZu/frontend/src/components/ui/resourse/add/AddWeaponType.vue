<template>
  <div class="add-weapon-wrap">
    <section class="card">
      <header class="card-head">
        <h3>{{ isEditMode ? '编辑武器类型' : '添加武器类型' }}</h3>
        <p class="subtitle">填写武器类型信息</p>
      </header>

      <div class="card-body form-grid">
        <label class="field">
          <span class="label">类型名称</span>
          <input v-model="form.type" type="text" :disabled="isEditMode" />
        </label>

        <label class="field">
          <span class="label">部署位置</span>
          <select v-model="form.deployDomain">
            <option value="地">地</option>
            <option value="海">海</option>
            <option value="天">天</option>
            <option value="空">空</option>
          </select>
        </label>

        <label class="field">
          <span class="label">装备作用</span>
          <select v-model="form.function">
            <option value="雷达">雷达</option>
            <option value="载弹">载弹</option>
            <option value="混合">混合</option>
          </select>
        </label>

        <label class="field">
          <span class="label">通道数量</span>
          <input v-model.number="form.channelCount" type="number" min="0" />
        </label>

        <div v-if="form.function === '载弹' || form.function === '混合'" class="field full">
          <span class="label">火力配置列表</span>
          <div class="fire-list">
            <div v-for="(ft, idx) in form.fireTypes" :key="idx" class="fire-item">
              <select v-model="ft.fireType" class="fire-input">
                <option v-for="option in missileOptions" :key="option" :value="option">{{ option }}</option>
              </select>
              <input v-model.number="ft.quantity" type="number" min="1" class="fire-qty" />
              <button type="button" class="btn remove" @click="removeFireType(idx)">删除</button>
            </div>
            <button type="button" class="btn add" @click="addFireType">+ 添加火力配置</button>
          </div>
        </div>

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
import { computed, ref, toRaw, watch } from 'vue';
import { resApi } from '@/api/resource';
import type { WeaponType } from '@/model/weaponType';
import { useResStore } from '@/stores/resource';

const props = defineProps<{
  initialValue?: WeaponType | null;
  mode?: 'add' | 'edit';
}>();

const store = useResStore();
const isEditMode = computed(() => props.mode === 'edit');
const message = ref('');
const error = ref('');
const loading = ref(false);

function defaultForm(): WeaponType {
  return {
    type: '',
    deployDomain: '地',
    function: '雷达',
    channelCount: 0,
    fireTypes: [],
    description: '',
  };
}

const form = ref<WeaponType>(defaultForm());

const missileOptions = computed(() => {
  return Array.from(store.fireTypeMap.values()).map(item => item.type);
});

watch(
  () => props.initialValue,
  value => {
    form.value = value
      ? {
          ...value,
          fireTypes: value.fireTypes ? value.fireTypes.map(item => ({ ...item })) : [],
        }
      : defaultForm();
    resetMessage();
  },
  { immediate: true },
);

watch(
  () => form.value.function,
  fn => {
    if (fn !== '载弹' && fn !== '混合') {
      form.value.fireTypes = [];
    }
  },
);

function resetMessage() {
  message.value = '';
  error.value = '';
}

function addFireType() {
  form.value.fireTypes = form.value.fireTypes || [];
  form.value.fireTypes.push({ fireType: '', quantity: 1 });
}

function removeFireType(index: number | string) {
  const i = Number(index);
  if (!Number.isFinite(i)) return;
  form.value.fireTypes?.splice(i, 1);
}

async function submitForm() {
  resetMessage();
  if (!form.value.type) {
    error.value = '请填写类型名称';
    return false;
  }

  loading.value = true;
  try {
    const raw = toRaw(form.value);
    const payload: WeaponType = {
      ...raw,
      fireTypes: (raw.fireTypes || []).map(ft => ({
        fireType: String(ft.fireType || ''),
        quantity: Number(ft.quantity || 0),
      })),
    };
    const res = await resApi.addWeaponType(payload);
    if (res && res.code === 200) {
      message.value = res.message || (isEditMode.value ? '保存成功' : '添加成功');
      return true;
    }
    error.value = res.message || '保存失败';
    return false;
  } catch (err: any) {
    error.value = err?.message || String(err);
    return false;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.value = props.initialValue
    ? {
        ...props.initialValue,
        fireTypes: props.initialValue.fireTypes ? props.initialValue.fireTypes.map(item => ({ ...item })) : [],
      }
    : defaultForm();
  resetMessage();
}

defineExpose({ submitForm, resetForm });
</script>

<style scoped>
.add-weapon-wrap { display:flex; justify-content:center; padding: 18px; }
.card { width: 100%; max-width: 780px; background: #fff; border-radius: 10px; box-shadow: 0 8px 24px rgba(16,24,40,0.06); overflow: hidden; }
.card-head { padding: 18px 20px; border-bottom: 1px solid #f1f5f9; font-size: 20px; }
.card-head h3 { margin: 0; font-size: 20px; }
.subtitle { margin: 6px 0 0 0; color: #6b7280; font-size: 14px; }
.card-body { padding: 18px 20px; font-size: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display:flex; flex-direction: column; }
.field.full { grid-column: 1 / -1; }
.label { font-size: 16px; color: #374151; margin-bottom: 6px; }
input[type='text'], input[type='number'] { padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 18px; color: #0f1724; }
select { padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 18px; }
textarea { padding: 8px 10px; border-radius: 8px; border: 1px solid #e6e9ef; font-size: 16px; color: #191e25; resize: vertical; }

.fire-list { display:flex; flex-direction: column; gap: 8px; }
.fire-item { display:flex; gap: 8px; align-items: center; }
.fire-input { flex: 1; padding: 6px 8px; border-radius: 6px; border: 1px solid #e6e9ef; font-size: 16px; }
.fire-qty { width: 84px; padding: 6px 8px; border-radius: 6px; border: 1px solid #e6e9ef; font-size: 16px; }
.btn { padding: 6px 10px; border-radius: 6px; border: none; background: #2563eb; color: #fff; cursor: pointer; }
.btn.remove { background: #ef4444; }
.btn.add { background: transparent; border: 1px dashed #cbd5e1; color: #6b7280; }

.msg { grid-column: 1 / -1; margin-top: 8px; }
.msg.success { color: #16a34a; }
.msg.error { color: #dc2626; }

@media (max-width: 680px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
