<!-- App.vue -->
<script setup lang="ts">
import { ref,onMounted,onUnmounted } from 'vue'
import MapContainer from './components/map/MapContainer.vue'
import { wsClient } from '@/utils/websocket';
import { useResStore } from './stores/resource';
import TestPage from './components/ui/test/TestPage.vue';
// 1. 定义状态：记录当前激活的菜单（用于同步给导航栏高亮）
const currentMenu = ref('') // 默认激活“战场初始化”

// 2. 定义地图容器的引用（用于向地图发送命令）
const mapContainerRef = ref<InstanceType<typeof MapContainer> | null>(null)

const resStore = useResStore();
onMounted(() => {
  // 全局启动连接
  wsClient.connect();
  resStore.loadAllDictionaries();
});

onUnmounted(() => {
  // 页面关闭时断开
  wsClient.close();
});

</script>

<template>
  <!-- <navigation_bar :activeMenu="currentMenu" 
    @menu-click="handleMenuClick" 
    @logo-click="handleLogoClick"></navigation_bar> -->
  <MapContainer ref="mapContainerRef" ></MapContainer>

  <!-- <TestPage></TestPage> -->
</template>

<style scoped></style>
