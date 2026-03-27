<script setup lang="ts">
import { ref ,onMounted,inject} from 'vue';
import L from 'leaflet';
import { simApi } from '@/api/simulation';
import ContextMenu from './sub-units/ContextMenu.vue';
import { type Map, type CircleMarker, circle } from 'leaflet';
import AddEnemyForm from '@/components/ui/state/add/AddEnemyForm.vue';
import ModalWindow from '@/components/ui/common/ModalWindow.vue';

// 菜单状态
const menuVisible = ref(false);
const menuPos = ref({ x: 0, y: 0 });
const clickLatLng = ref<L.LatLng | null>(null); // 记录右键点击的经纬度
const map = inject<Map>('map'); // 确保地图实例已注入
if (!map) {
  throw new Error('Map instance not provided via inject("map")');
}

// 初始化地图时绑定事件
onMounted(() => {
  // --- 核心：绑定右键事件 ---
  map.on('contextmenu', (e: L.LeafletMouseEvent) => {
    // 1. 记录经纬度 (发给后端用)
    clickLatLng.value = e.latlng;
    
    // 2. 记录屏幕坐标 (显示菜单用)
    menuPos.value = { x: e.originalEvent.clientX, y: e.originalEvent.clientY };
    
    // 3. 显示菜单
    menuVisible.value = true;
  });
  
  // 地图拖动时关闭菜单
  map.on('move', () => menuVisible.value = false);
});

// 弹窗状态
const showAddEnemyModal = ref(false);
const selectedLocation = ref({ lat: 0, lng: 0 }); // 暂存右键点击的位置


// 处理菜单选择
const handleMenuSelect = async (action: string) => {
  menuVisible.value = false; // 选完关闭
  
  if (!clickLatLng.value) return;
  const { lat, lng } = clickLatLng.value;

    // 记录坐标，供弹窗使用
  selectedLocation.value = { 
    lat: clickLatLng.value.lat, 
    lng: clickLatLng.value.lng 
  };

  if (action === 'add-enemy') {
    showAddEnemyModal.value = true; // 开弹窗
  }
  
  if (action === 'add-zone') {

  }
};



// 2. 处理表单提交
const handleEnemySubmit = async (formData: any) => {

};
</script>

<template>
  <div id="map"></div>
  
  <ContextMenu 
    :visible="menuVisible"
    :x="menuPos.x"
    :y="menuPos.y"
    @close="menuVisible = false"
    @select="handleMenuSelect"
  />
  <!-- 添加敌方弹窗 -->
  <ModalWindow 
    :show="showAddEnemyModal" 
    title="部署敌方目标" 
    width="600px"
    @close="showAddEnemyModal = false"
  >
    <!-- 嵌入表单组件，把坐标传进去 -->
    <AddEnemyForm 
      :lat="selectedLocation.lat"
      :lng="selectedLocation.lng"
      @submit="handleEnemySubmit"
      @cancel="showAddEnemyModal = false"
    />
  </ModalWindow>
</template>