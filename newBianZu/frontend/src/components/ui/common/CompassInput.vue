<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';

const props = defineProps<{
  compassAngle: number; // 接收外部传入的角度 (v-model)
}>();

const emit = defineEmits(['update:compassAngle']);

const compassRef = ref<HTMLElement | null>(null);
const isDragging = ref(false);

// 本地可编辑角度，用于 input 双向绑定
const localAngle = ref<number>(props.compassAngle ?? 0);

// 同步外部 prop 到本地（当外部通过拖拽或外部更新时）
watch(
  () => props.compassAngle,
  (v) => {
    if (typeof v === 'number' && v !== localAngle.value) {
      localAngle.value = v;
    }
  }
);

// 当用户在文本框提交角度时，标准化并发出更新
function commitLocalAngle() {
  let a = Number(localAngle.value);
  if (!Number.isFinite(a) || isNaN(a)) a = 0;
  // 归一到 0-359
  a = ((Math.round(a) % 360) + 360) % 360;
  localAngle.value = a;
  emit('update:compassAngle', a);
}

// 计算角度的核心逻辑
const calculateAngle = (event: MouseEvent | TouchEvent) => {
  if (!compassRef.value) return;

  const rect = compassRef.value.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 2;

  // 获取鼠标/触摸点坐标
  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX;
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY;

  // 计算向量 (dx, dy)
  // 注意屏幕坐标系：Y轴向下。我们需要把坐标系转为数学坐标系
  const dx = clientX - centerX;
  const dy = clientY - centerY;

  // atan2 返回弧度 (-PI 到 PI)
  // 0度通常是正右(X轴)，但地图航向 0度是正北(Y轴负方向)
  // 我们需要做一个转换：Heading = atan2(x, -y) * (180/PI)
  let angle = Math.atan2(dx, -dy) * (180 / Math.PI);

  // 转为 0-360 范围
  if (angle < 0) angle += 360;

  // 取整并发送
  const rounded = Math.round(angle);
  emit('update:compassAngle', rounded);
  // 同步本地输入显示，避免延迟不同步
  localAngle.value = rounded;
};

// 事件监听
const startDrag = (e: MouseEvent) => {
  isDragging.value = true;
  calculateAngle(e); // 点击瞬间也更新
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
};

const onDrag = (e: MouseEvent) => {
  if (isDragging.value) calculateAngle(e);
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};
</script>

<template>
  <div class="compass-wrapper">
    <!-- 圆盘背景 -->
    <div 
      class="compass-dial" 
      ref="compassRef"
      @mousedown="startDrag"
    >
      <!-- 刻度线 (装饰) -->
      <div class="mark n">N</div>
      <div class="mark e">E</div>
      <div class="mark s">S</div>
      <div class="mark w">W</div>

      <!-- 指针 (根据 modelValue 旋转) -->
      <div 
        class="pointer" 
        :style="{ transform: `rotate(${props.compassAngle}deg)` }"
      >
        <div class="arrow-head"></div>
      </div>
    </div>
    
    <!-- 可编辑数字显示 -->
    <div class="value-display">
      <input
        class="angle-input"
        type="number"
        v-model.number="localAngle"
        @change="commitLocalAngle"
        @blur="commitLocalAngle"
        min="0"
        max="359"
        aria-label="航向角度"
      />
      <span class="deg">°</span>
    </div>
  </div>
</template>

<style scoped>
.compass-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.compass-dial {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #f0f0f0;
  border: 4px solid #ddd;
  position: relative;
  cursor: pointer;
  user-select: none; /* 防止拖拽选中文本 */
}

/* 装饰文字 */
.mark {
  position: absolute;
  font-size: 12px;
  font-weight: bold;
  color: #999;
}
.mark.n { top: 5px; left: 50%; transform: translateX(-50%); }
.mark.s { bottom: 5px; left: 50%; transform: translateX(-50%); }
.mark.e { right: 5px; top: 50%; transform: translateY(-50%); }
.mark.w { left: 5px; top: 50%; transform: translateY(-50%); }

/* 指针容器 (旋转中心在圆心) */
.pointer {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  /* 此时 pointer 覆盖整个圆，不需要 transform-origin，CSS rotate 会围绕中心旋转 */
  transition: transform 0.1s linear; /* 增加一点平滑 */
}

/* 箭头图形 */
.arrow-head {
  width: 0; 
  height: 0; 
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 20px solid #2196F3; /* 箭头颜色 */
  position: absolute;
  top: 15px; /* 距离顶部一点距离 */
  left: 50%;
  transform: translateX(-50%);
}

.value-display { display:flex; align-items:center; gap:6px; }
.angle-input {
  width: 40px;
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-weight: bold;
  font-size: 16px;
  text-align: right;
}
.deg { font-weight: bold; color: #333; }
</style>