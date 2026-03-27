<template>
  <div class="control-panel" role="region" aria-label="仿真控制面板">
    <div class="center">
      <button class="main-btn" @click="toggleRunning" :aria-pressed="running" :title="running ? '暂停仿真' : '停止仿真'">
        <span class="power-symbol" aria-hidden="true">{{ running ? '❚❚' : '▶' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import axios from 'axios'; // 记得引入 axios
import { simApi } from '@/api/simulation';
const emit = defineEmits(['running-change']);

const running = ref(false);
// 核心逻辑
async function toggleRunning() {
  // 1. 决定当前动作：如果现在是 running，那就要发 stop；反之发 start
  const action = running.value ? 'stop' : 'start';
  
  try {
    // 2. 发送请求给后端
    const res = await simApi.toggleSimulation(action);
    // 3. 只有后端返回成功了，才切换前端 UI 状态
    // 简单的判断逻辑：看后端返回的字符串里有没有 "success" (根据你后端 Controller 的返回值调整)
    if (res.code === 200) {
      // 切换状态
      running.value = !running.value;

      // 通知父组件
      emit('running-change', running.value);

      console.log(`仿真已切换为: ${running.value ? '运行中' : '停止'}`);
    } else {
      console.warn('后端拒绝了请求:', res);
      alert('切换失败: 后端可能未就绪');
    }
    
  } catch (error) {
    console.error("网络请求失败:", error);
    // 这里 running.value 没有变，所以按钮不会变色，符合预期
    alert('网络错误，无法连接仿真服务');
  }
}

</script>

<style scoped>
.control-panel {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 200px; /* 缩小尺寸 */
  height: 200px; /* 缩小尺寸 */
  border-radius: 50%;
  background: linear-gradient(135deg, #6b7280, #4b5563); /* 灰色调 */
  box-shadow: 0 10px 28px rgba(0,0,0,0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  user-select: none;
}
.center {
  text-align: center;
  color: #fff;
}
.main-btn {
  background: rgba(255,255,255,0.06);
  border: none;
  color: #fff;
  width: 120px; /* 中心开关大小 */
  height: 120px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.08s ease, background 0.12s ease;
  box-shadow: 0 6px 18px rgba(0,0,0,0.20) inset;
  font-size: 36px;
}
.main-btn:active { transform: scale(0.98); }
.power-symbol {
  font-size: 48px;
  line-height: 1;
  color: #fff;
}
.main-btn[aria-pressed="true"] {
  background: linear-gradient(135deg, #10b981, #059669); /* 运行时中心变绿色 */
}
.main-btn[aria-pressed="false"]{
  /* 暂停/停止时红色背景，保持其他样式不变 */
  background: linear-gradient(180deg, #ef4444, #b91c1c);
  color: #ffffff;
}

/* 适配小屏幕，按比例缩小 */
@media (max-width: 420px) {
  .control-panel {
    width: 160px;
    height: 160px;
    right: 14px;
    bottom: 14px;
  }
  .main-btn { width: 96px; height: 96px; font-size: 30px; }
  .power-symbol { font-size: 40px; }
}
</style>
