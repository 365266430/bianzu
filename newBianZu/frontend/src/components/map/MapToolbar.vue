<template>
  <div class="map-toolbar">
    <div class="logo-area">
      <!-- 可替换为你的logo图片 -->
      <span class="logo-text">作战编组</span>
    </div>
    <div class="toolbar-btn-group" @mouseenter="showSub1 = true" @mouseleave="showSub1 = false">
      <button class="toolbar-btn" @click="$emit('node-info-click')">节点信息</button>
      <div v-if="showSub1" class="sub-btn-list">
        <button class="sub-btn" @click="$emit('resource-type-info')"><span class="sub-btn-inner">资源类型数据库</span></button>
      </div>
    </div>
    <div class="toolbar-btn-group" @mouseenter="showSub2 = true" @mouseleave="showSub2 = false">
      <button class="toolbar-btn">静态编组</button>
      <div v-if="showSub2" class="sub-btn-list">
        <button class="sub-btn" @click="$emit('pso-fire-allot')"><span class="sub-btn-inner">静态火力分配</span></button>
        <button class="sub-btn" @click="$emit('static-group-generate')"><span class="sub-btn-inner">静态编组生成</span></button>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref } from 'vue'
const showSub1 = ref(false)
const showSub2 = ref(false)
</script>

<style scoped>
.map-toolbar {
  display: flex;
  gap: 18px;
  padding: 12px 24px;
  background: rgba(44, 62, 80, 0.9);
  border-radius: 0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1001;
  align-items: center;
}

.logo-area {
  display: flex;
  align-items: center;
  margin-right: 24px;
  min-width: 60px;
  height: 36px;
}
.logo-text {
  font-size: 1.25rem;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2px;
}

.toolbar-btn-group {
  position: relative;
  display: flex;
  flex-direction: column;
}

.toolbar-btn {
  padding: 8px 28px;
  background: linear-gradient(90deg, #1a4a7c 0%, #2766a3 100%);
  color: #fff;
  border: none;
  border-radius: 0;
  font-weight: 600;
  font-size: 1rem;
  letter-spacing: 1px;
  box-shadow: 0 2px 8px rgba(52,152,219,0.15);
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
  outline: none;
  margin-left: 0;
  min-width: 180px; /* 与子按钮宽度一致 */
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}
.toolbar-btn:hover {
  background: linear-gradient(90deg, #14365c 0%, #1e4e7a 100%);
  transform: translateY(-2px) scale(1.05);
}

.sub-btn-list {
  position: absolute;
  top: 100%;
  left: 0;
  display: flex;
  flex-direction: column;
  background: rgba(44, 62, 80, 0.98);
  box-shadow: 0 2px 8px rgba(52,152,219,0.15);
  padding: 4px 0;
  min-width: 180px;
}

.sub-btn {
  padding: 6px 18px;
  background: #1a4a7c;
  color: #fff;
  border: none;
  border-radius: 0;
  font-size: 0.95rem;
  cursor: pointer;
  text-align: center;
  transition: background 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 180px;
}
.sub-btn:hover {
  background: #14365c;
}
.sub-btn-inner {
  width: 100%;
  text-align: center;
  display: inline-block;
}
</style>