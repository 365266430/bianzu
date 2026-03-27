<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps<{
  visible: boolean;
  x: number;
  y: number;
}>();

const emit = defineEmits(['close', 'select']);

// 点击菜单外部自动关闭
const handleClickOutside = () => {
  if (props.visible) emit('close');
};

onMounted(() => document.addEventListener('click', handleClickOutside));
onUnmounted(() => document.removeEventListener('click', handleClickOutside));
</script>

<template>
  <div 
    v-if="visible" 
    class="context-menu" 
    :style="{ top: y + 'px', left: x + 'px' }"
    @click.stop
  >
    <ul>
      <li @click="emit('select', 'add-enemy')">+ 添加敌方目标</li>
      <li @click="emit('select', 'add-zone')">+ 添加保护区</li>
      <li class="separator"></li>
      <li @click="emit('select', 'clear')">🗑️ 清除所有</li>
    </ul>
  </div>
</template>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 9999; /* 必须比地图高 */
  background: rgb(255, 255, 255);
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  min-width: 150px;
  padding: 5px 0;
}
.context-menu ul {
  list-style: none; margin: 0; padding: 0;
}
.context-menu li {
  padding: 8px 15px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}
.context-menu li:hover {
  background-color: #f0f0f0;
  color: #007bff;
}
.separator {
  border-top: 1px solid #eee;
  margin: 4px 0;
  padding: 0 !important;
}
</style>