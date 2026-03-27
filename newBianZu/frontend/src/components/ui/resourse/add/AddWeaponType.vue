<template>
  <div class="add-weapon-wrap">
    <section class="card">
      <header class="card-head">
        <h3>添加武器类型</h3>
        <p class="subtitle">填写武器类型信息</p>
      </header>

      <div class="card-body form-grid">
        <label class="field">
          <span class="label">类型名称</span>
          <input v-model="form.type" type="text" />
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

        <!-- 火力配置列表 -->
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
import { ref, computed, watch ,toRaw} from 'vue';
import { useResStore } from '@/stores/resource';
import { resApi } from '@/api/resource';

const store = useResStore();

const message = ref('');
const error = ref('');
const loading = ref(false);

const form = ref<any>({
  type: '',
  deployDomain: '地',
  function: '雷达',
  channelCount: 0,
  fireTypes: [] as Array<{ fireType: string; quantity: number }> ,
  description: '这是一个武器类型'
});

function resetMessage() {
  message.value = '';
  error.value = '';
}

function addFireType() {
  form.value.fireTypes.push({ fireType: '', quantity: 1 });
}

function removeFireType(index: number | string) {
  const i = Number(index);
  if (!Number.isFinite(i)) return;
  form.value.fireTypes.splice(i, 1);
}

// 计算可选的导弹 fireType 列表：优先使用带 isMissile 标记的项，否则返回全部
const missileOptions = computed(() => {
  const arr = Array.from((store.fireTypeMap && store.fireTypeMap.values) ? store.fireTypeMap.values() : [] as any);
  // values() 可能返回 Iterator; convert properly
  const list = Array.isArray(arr) ? arr : Array.from(arr as Iterable<any>);
  if (list.length === 0) return [] as string[];
  return list.map((f: any) => f.type);
});

// 当装备作用变为非载弹/混合时，自动清空 fireTypes，防止用户填写的配置保留
watch(() => form.value.function, (fn) => {
  if (fn !== '载弹' && fn !== '混合') {
    form.value.fireTypes = [];
  }
});

async function submitForm() {
    resetMessage();
    // 简单校验
    if (!form.value.type) {
        error.value = '请填写类型名称';
        return false;
    }
    try {
        const raw = toRaw(form.value);
        const payload = {
        ...raw,
        fireTypes: (raw.fireTypes || []).map((ft: any) => ({
            fireType: String(ft.fireType || ''),
            quantity: Number(ft.quantity || 0)
        }))
        };
        const res = await resApi.addWeaponType(payload);
        if(res && res.code == 200){
            message.value = res.message || '武器类型添加成功';
            resetForm();
            return true;
        } else {
            error.value = res.message || '武器类型添加失败';
            throw new Error(res.message || '武器类型添加失败');
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
  form.value = {
    type: '',
    deployDomain: '地',
    function: '雷达',
    channelCount: 0,
    fireTypes: [],
    description: ''
  };
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