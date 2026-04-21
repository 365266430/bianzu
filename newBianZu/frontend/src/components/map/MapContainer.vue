<template>
  <MapToolbar
    @resource-type-info="showResourceModal = true"
    @pso-fire-allot="openFormationModal('allocation')"
    @static-group-generate="openFormationModal('plans')"
    @dynamic-group-generate="showDynamicFormationModal = true"
  />

  <div ref="mapContainer" class="w-full h-screen"></div>
  <MapMarkers v-if="mapLoaded" />
  <MapControls v-if="mapLoaded" />

  <ModalWindow
    :show="showResourceModal"
    title="战场资源类型数据库"
    width="800px"
    height="600px"
    @close="showResourceModal = false"
  >
    <ResourceDashboard />
  </ModalWindow>

  <ModalWindow
    :show="showFormationModal"
    title="静态编组与火力分配"
    width="1260px"
    height="82vh"
    max-height="82vh"
    @close="showFormationModal = false"
  >
    <StaticFormationPanel :default-tab="formationTab" />
  </ModalWindow>

  <ModalWindow
    :show="showDynamicFormationModal"
    title="生成动态编组方案"
    width="1400px"
    height="86vh"
    max-height="86vh"
    @close="showDynamicFormationModal = false"
  >
    <DynamicFormationPanel />
  </ModalWindow>

  <ControlPanel />
</template>

<script setup>
import { inject, onMounted, provide, ref, shallowRef } from 'vue'
import { createMap } from '@/plugins/leaflet'
import MapMarkers from './MapMarkers.vue'
import MapControls from './MapControls.vue'
import MapToolbar from './MapToolbar.vue'
import ControlPanel from './sub-units/ControlPanel.vue'
import ResourceDashboard from '../ui/resourse/ResourceDashboard.vue'
import StaticFormationPanel from '../ui/formation/StaticFormationPanel.vue'
import DynamicFormationPanel from '../ui/formation/DynamicFormationPanel.vue'
import ModalWindow from '../ui/common/ModalWindow.vue'

const mapContainer = ref(null)
const L = inject('L')
const map = shallowRef(null)
const mapLoaded = ref(false)
const showResourceModal = ref(false)
const showFormationModal = ref(false)
const showDynamicFormationModal = ref(false)
const formationTab = ref('allocation')

function openFormationModal(tab) {
  formationTab.value = tab
  showFormationModal.value = true
}

onMounted(() => {
  if (L && mapContainer.value) {
    map.value = createMap(mapContainer.value, {
      zoomControl: false,
      zoom: 5,
    })
    provide('map', map.value)
    mapLoaded.value = true
  }
})
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
