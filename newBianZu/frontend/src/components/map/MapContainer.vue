<!-- views/main_map.vue -->
<template>
  <MapToolbar
      @resource-type-info="showResourceModal = true"
  />
  <div ref="mapContainer" class="w-full h-screen"></div>
  <!-- 只有在地图加载完成后才渲染 MapMarkers -->
  <MapMarkers v-if="mapLoaded" :protectors="protectors"></MapMarkers>
  <MapControls v-if="mapLoaded" :protectors="protectors"/>
  <ModalWindow :show="showResourceModal" @close="showResourceModal = false" title="战场资源类型数据库" width="800px" height="600px">
    <ResourceDashboard/>
  </ModalWindow>
  <ControlPanel />
  <!-- <MarkerManageWindow v-if="isMarkerManageWindow" @generate-protector="handleNewProtector" ></MarkerManageWindow> -->
</template>

<script setup>
import { ref, inject, onMounted, provide, shallowRef } from 'vue';
import { createMap } from '@/plugins/leaflet';
import MapMarkers from './MapMarkers.vue';
import MapControls from './MapControls.vue';
import MapToolbar from './MapToolbar.vue';
import ControlPanel from './sub-units/ControlPanel.vue';
import ResourceDashboard from '../ui/resourse/ResourceDashboard.vue';
import ModalWindow from '../ui/common/ModalWindow.vue';

const mapContainer = ref(null);
const L = inject('L');
const map = shallowRef(null);
const mapLoaded = ref(false);

// 控制战场资源类型数据库模态框显示
const showResourceModal = ref(false);


onMounted(() => {
  if (L && mapContainer.value) {
    map.value = createMap(mapContainer.value, {
      zoomControl: false, // 关键配置，关闭默认缩放控制
      zoom: 4
    });
    // 提供地图实例
    provide('map', map.value);
    // 标记地图已加载
    mapLoaded.value = true;
  }

});

</script>

<style scoped>
.w-full {
  width: 100%;
  cursor: crosshair;
}

.h-screen {
  height: 100vh;
}
</style>