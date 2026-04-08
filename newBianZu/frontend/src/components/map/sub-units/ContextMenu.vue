<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  targetKind: 'map' | 'enemy' | 'zone'
}>()

const emit = defineEmits<{
  close: []
  select: [action: string]
}>()

const handleClickOutside = () => {
  if (props.visible) emit('close')
}

onMounted(() => {
  window.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div
    v-if="visible"
    class="context-menu"
    :style="{ top: y + 'px', left: x + 'px' }"
    @click.stop
  >
    <ul v-if="targetKind === 'map'">
      <li @click="emit('select', 'add-enemy')">+ 添加敌方目标</li>
      <li @click="emit('select', 'add-zone')">+ 添加保护区</li>
      <li class="separator"></li>
      <li class="danger" @click="emit('select', 'clear')">清除所有</li>
    </ul>

    <ul v-else-if="targetKind === 'enemy'">
      <li @click="emit('select', 'edit-enemy')">编辑红方节点</li>
      <li class="separator"></li>
      <li class="danger" @click="emit('select', 'delete-enemy')">删除红方节点</li>
      <li class="separator"></li>
      <li class="danger" @click="emit('select', 'clear')">清除所有</li>
    </ul>

    <ul v-else>
      <li @click="emit('select', 'edit-zone')">编辑保护区</li>
      <li class="separator"></li>
      <li class="danger" @click="emit('select', 'delete-zone')">删除保护区</li>
      <li class="separator"></li>
      <li class="danger" @click="emit('select', 'clear')">清除所有</li>
    </ul>
  </div>
</template>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  min-width: 160px;
  padding: 5px 0;
}

.context-menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
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

.context-menu li.danger:hover {
  color: #c62828;
}

.separator {
  border-top: 1px solid #eee;
  margin: 4px 0;
  padding: 0 !important;
}
</style>
