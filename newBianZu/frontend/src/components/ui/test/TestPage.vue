<template>
  <div class="test-page">
    <h3>接口请求测试</h3>
    <div class="controls">
      <button class="btn" @click="onTestRequest" :disabled="loading">{{ loading ? '请求中...' : '测试获取敌人列表' }}</button>
      <button class="btn ghost" @click="clear">清除</button>
    </div>

    <div class="result">
      <div v-if="error" class="error">错误: {{ error }}</div>
      <div v-else-if="!response">尚无响应，点击按钮开始测试</div>
      <div v-else>
        <p>收到响应：共 {{ (response && response.length) || '未知' }} 条记录</p>
        <pre class="resp">{{ pretty }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { testApi } from '@/api/test';

const loading = ref(false);
const error = ref('');
const response = ref<any | null>(null);

async function onTestRequest() {
  loading.value = true;
  error.value = '';
  response.value = null;
  try {
    const res = await testApi.testConnection();
    console.log('请求成功', res);
    response.value = res;
  } catch (err: any) {
    console.error('请求失败', err);
    error.value = err?.message || String(err);
  } finally {
    loading.value = false;
  }
}

function clear() {
  response.value = null;
  error.value = '';
}

const pretty = computed(() => {
  try {
    return JSON.stringify(response.value, null, 2);
  } catch {
    return String(response.value);
  }
});
</script>

<style scoped>
.test-page { padding: 16px; }
.controls { display:flex; gap:10px; margin-bottom:12px; }
.btn { padding: 8px 12px; border-radius:6px; border:1px solid #ccc; background:#fff; cursor:pointer; }
.btn[disabled] { opacity:0.6; cursor:not-allowed; }
.btn.ghost { background:transparent; }
.result { margin-top: 10px; }
.error { color: #b91c1c; font-weight:600; }
.resp { background:#0f1724; color:#e6eef8; padding:12px; border-radius:6px; overflow:auto; max-height:40vh; }
</style>