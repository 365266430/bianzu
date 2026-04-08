<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, inject } from 'vue'
import L from 'leaflet'
import type { Map } from 'leaflet'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import { simApi } from '@/api/simulation'
import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import AddEnemyForm from '@/components/ui/state/add/AddEnemyForm.vue'
import AddZoneForm from '@/components/ui/state/add/AddZoneForm.vue'
import ModalWindow from '@/components/ui/common/ModalWindow.vue'
import ContextMenu from './sub-units/ContextMenu.vue'
import { MAP_CONTEXT_MENU_EVENT, type MapContextMenuDetail } from '@/utils/mapContextMenu'

const menuVisible = ref(false)
const menuPos = ref({ x: 0, y: 0 })
const selectedLocation = ref({ lat: 0, lng: 0 })
const showEnemyModal = ref(false)
const showZoneModal = ref(false)
const enemyModalMode = ref<'create' | 'edit'>('create')
const zoneModalMode = ref<'create' | 'edit'>('create')
const targetKind = ref<'map' | 'enemy' | 'zone'>('map')
const selectedTargetId = ref<string | null>(null)
const selectedEnemy = ref<EnemyNode | null>(null)
const selectedZone = ref<ProtectionZone | null>(null)
let suppressMapContextMenuUntil = 0

const map = inject<Map>('map')
if (!map) {
  throw new Error('Map instance not provided via inject("map")')
}

const enemyInitialValue = computed(() => {
  if (!selectedEnemy.value) return null
  return {
    name: selectedEnemy.value.id,
    type: selectedEnemy.value.type,
    heading: Number(selectedEnemy.value.heading ?? 90),
    altitude: Number(selectedEnemy.value.altitude ?? 10000),
    speed: Number(selectedEnemy.value.speed ?? 0),
    count: 1,
  }
})

const zoneInitialValue = computed(() => {
  if (!selectedZone.value) return null
  return {
    name: String(selectedZone.value.id ?? ''),
    size: Number(selectedZone.value.size ?? 25000),
    value: Number(selectedZone.value.value ?? 2) as 1 | 2 | 3,
    health: Number(selectedZone.value.health ?? 10),
  }
})

const enemyModalTitle = computed(() => enemyModalMode.value === 'edit' ? '编辑红方节点' : '部署敌方目标')
const zoneModalTitle = computed(() => zoneModalMode.value === 'edit' ? '编辑保护区' : '添加保护区')
const enemySubmitLabel = computed(() => enemyModalMode.value === 'edit' ? '保存' : '生成')
const zoneSubmitLabel = computed(() => zoneModalMode.value === 'edit' ? '保存' : '生成')

const handleObjectContextMenu = (event: Event) => {
  const customEvent = event as CustomEvent<MapContextMenuDetail>
  suppressMapContextMenuUntil = Date.now() + 150
  menuPos.value = { x: customEvent.detail.x, y: customEvent.detail.y }
  targetKind.value = customEvent.detail.target.kind
  selectedTargetId.value = customEvent.detail.target.kind === 'map' ? null : customEvent.detail.target.id

  if (customEvent.detail.target.kind === 'enemy') {
    selectedEnemy.value = customEvent.detail.target.enemy
    selectedZone.value = null
    selectedLocation.value = {
      lat: Number(customEvent.detail.target.enemy.latitude),
      lng: Number(customEvent.detail.target.enemy.longitude),
    }
  } else if (customEvent.detail.target.kind === 'zone') {
    selectedZone.value = customEvent.detail.target.zone
    selectedEnemy.value = null
    selectedLocation.value = {
      lat: Number(customEvent.detail.target.zone.location?.[1] ?? 0),
      lng: Number(customEvent.detail.target.zone.location?.[0] ?? 0),
    }
  }

  menuVisible.value = true
}

onMounted(() => {
  map.on('contextmenu', (e: L.LeafletMouseEvent) => {
    if (Date.now() < suppressMapContextMenuUntil) {
      return
    }

    selectedLocation.value = {
      lat: e.latlng.lat,
      lng: e.latlng.lng,
    }
    menuPos.value = {
      x: e.originalEvent.clientX,
      y: e.originalEvent.clientY,
    }
    targetKind.value = 'map'
    selectedTargetId.value = null
    selectedEnemy.value = null
    selectedZone.value = null
    menuVisible.value = true
  })

  map.on('move', () => {
    menuVisible.value = false
  })

  window.addEventListener(MAP_CONTEXT_MENU_EVENT, handleObjectContextMenu as EventListener)
})

onUnmounted(() => {
  window.removeEventListener(MAP_CONTEXT_MENU_EVENT, handleObjectContextMenu as EventListener)
})

const handleMenuSelect = async (action: string) => {
  menuVisible.value = false

  if (action === 'add-enemy') {
    enemyModalMode.value = 'create'
    showEnemyModal.value = true
    return
  }

  if (action === 'add-zone') {
    zoneModalMode.value = 'create'
    showZoneModal.value = true
    return
  }

  if (action === 'edit-enemy' && selectedEnemy.value) {
    enemyModalMode.value = 'edit'
    showEnemyModal.value = true
    return
  }

  if (action === 'edit-zone' && selectedZone.value) {
    zoneModalMode.value = 'edit'
    showZoneModal.value = true
    return
  }

  if (action === 'delete-enemy' && selectedTargetId.value) {
    await deleteEnemy(selectedTargetId.value)
    return
  }

  if (action === 'delete-zone' && selectedTargetId.value) {
    await deleteZone(selectedTargetId.value)
    return
  }

  if (action === 'clear') {
    await clearAllObjects()
  }
}

const handleEnemySubmit = async (formData: any) => {
  const payload: EnemyNode = {
    id: formData.name?.trim() || selectedEnemy.value?.id || '',
    type: formData.type,
    longitude: Number(formData.lng),
    latitude: Number(formData.lat),
    altitude: Number(formData.altitude ?? 10000),
    heading: Number(formData.heading ?? 90),
    speed: Number(formData.speed ?? 0),
  }

  try {
    const res = enemyModalMode.value === 'edit' && selectedTargetId.value
      ? await simApi.updateEnemyNode(selectedTargetId.value, payload)
      : await simApi.addEnemyNode(payload)

    if (res.code !== 200) {
      throw new Error(res.message || (enemyModalMode.value === 'edit' ? 'Failed to update enemy target' : 'Failed to create enemy target'))
    }
    showEnemyModal.value = false
    ElMessage.success(enemyModalMode.value === 'edit' ? '红方节点信息已更新' : '敌方目标已生成')
  } catch (error: any) {
    console.error('Failed to save enemy target:', error)
    alert(error?.message || 'Failed to save enemy target. Please try again.')
  }
}

const handleZoneSubmit = async (formData: any) => {
  const payload: ProtectionZone = {
    id: formData.name?.trim() || selectedZone.value?.id || '',
    location: [Number(formData.lng), Number(formData.lat)],
    size: Number(formData.size ?? 25000),
    value: Number(formData.value ?? 2) as 1 | 2 | 3,
    health: Number(formData.health ?? 10),
    stationedWeaponIds: selectedZone.value?.stationedWeaponIds ?? [],
  }

  try {
    const res = zoneModalMode.value === 'edit' && selectedTargetId.value
      ? await simApi.updateProtectionZone(selectedTargetId.value, payload)
      : await simApi.addProtectionZone(payload)

    if (res.code !== 200) {
      throw new Error(res.message || (zoneModalMode.value === 'edit' ? 'Failed to update protection zone' : 'Failed to create protection zone'))
    }
    showZoneModal.value = false
    ElMessage.success(zoneModalMode.value === 'edit' ? '保护区信息已更新' : '保护区已生成')
  } catch (error: any) {
    console.error('Failed to save protection zone:', error)
    alert(error?.message || 'Failed to save protection zone. Please try again.')
  }
}

async function deleteEnemy(enemyId: string) {
  try {
    const res = await simApi.deleteEnemyNode(enemyId)
    if (res.code !== 200) {
      throw new Error(res.message || 'Failed to delete enemy target')
    }
  } catch (error: any) {
    console.error('Failed to delete enemy target:', error)
    alert(error?.message || 'Failed to delete enemy target. Please try again.')
  }
}

async function deleteZone(zoneId: string) {
  try {
    const res = await simApi.deleteProtectionZone(zoneId)
    if (res.code !== 200) {
      throw new Error(res.message || 'Failed to delete protection zone')
    }
  } catch (error: any) {
    console.error('Failed to delete protection zone:', error)
    alert(error?.message || 'Failed to delete protection zone. Please try again.')
  }
}

async function clearAllObjects() {
  try {
    const res = await simApi.clearAllObjects()
    if (res.code !== 200) {
      throw new Error(res.message || 'Failed to clear map objects')
    }
  } catch (error: any) {
    console.error('Failed to clear map objects:', error)
    alert(error?.message || 'Failed to clear map objects. Please try again.')
  }
}
</script>

<template>
  <div id="map"></div>

  <ContextMenu
    :visible="menuVisible"
    :x="menuPos.x"
    :y="menuPos.y"
    :target-kind="targetKind"
    @close="menuVisible = false"
    @select="handleMenuSelect"
  />

  <ModalWindow
    :show="showEnemyModal"
    :title="enemyModalTitle"
    width="600px"
    @close="showEnemyModal = false"
  >
    <AddEnemyForm
      :lat="selectedLocation.lat"
      :lng="selectedLocation.lng"
      :initial-value="enemyModalMode === 'edit' ? enemyInitialValue : null"
      :submit-label="enemySubmitLabel"
      @submit="handleEnemySubmit"
      @cancel="showEnemyModal = false"
    />
  </ModalWindow>

  <ModalWindow
    :show="showZoneModal"
    :title="zoneModalTitle"
    width="520px"
    @close="showZoneModal = false"
  >
    <AddZoneForm
      :lat="selectedLocation.lat"
      :lng="selectedLocation.lng"
      :initial-value="zoneModalMode === 'edit' ? zoneInitialValue : null"
      :submit-label="zoneSubmitLabel"
      @submit="handleZoneSubmit"
      @cancel="showZoneModal = false"
    />
  </ModalWindow>
</template>
