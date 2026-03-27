<script setup lang="ts">
// 定义 Props，让父组件传标题和控制显示
defineProps<{
  show: boolean;    // 是否显示
  title?: string;   // 标题（可选）
  width?: string;   // 宽度（可选，默认 600px）
  height?: string;  // 高度（可选，默认 auto）
  maxHeight?: string; // 最大高度（可选）
}>();

// 定义 Emits，通知父组件关闭
const emit = defineEmits(['close']);

const close = () => {
  emit('close');
};
</script>

<template>
  <!-- 动画过渡 (Vue 内置) -->
  <Transition name="fade">
    <!-- 1. 外层遮罩 -->
    <div v-if="show" class="modal-overlay" @click.self="close">
      
      <!-- 2. 窗口主体 -->
      <div class="modal-window" :style="{ width: width || '600px' , height: height || 'auto' , maxHeight: maxHeight || '80vh' }">
        
        <!-- A. 标题栏 -->
        <header class="modal-header">
          <h3>{{ title || '窗口' }}</h3>
          <button class="close-btn" @click="close">×</button>
        </header>

        <!-- B. 内容区 (插槽) -->
        <div class="modal-content">
          <slot></slot>
        </div>

        <!-- C. 底部 (可选插槽) -->
        <div class="modal-footer" v-if="$slots.footer">
          <slot name="footer"></slot>
        </div>
        
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* 遮罩层：全屏固定，半透明黑 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000; /* 保证在最上层 */
}

/* 窗口主体 */
.modal-window {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 90vh; /* 防止太高溢出屏幕 */
}

/* 标题栏 */
.modal-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-btn {
  background: none; border: none;
  font-size: 24px; cursor: pointer; color: #999;
}
.close-btn:hover { color: #333; }

/* 内容区：可滚动 */
.modal-content {
  padding: 20px;
  overflow-y: auto; 
}

/* Vue 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>