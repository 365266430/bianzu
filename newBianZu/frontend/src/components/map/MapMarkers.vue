<template>
  <div></div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, inject } from 'vue'
import L from 'leaflet'
import type { Map, CircleMarker, Layer } from 'leaflet'
import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import { wsClient } from '@/utils/websocket'
import { createEnemy, createZone } from '@/composables/useMarkers'
import { dispatchMapContextMenu } from '@/utils/mapContextMenu'

const map = inject<Map>('map')
if (!map) {
  throw new Error('Map instance not provided via inject("map")')
}

let enemyMarkers: CircleMarker[] = []
let zoneLayers: Layer[] = []

const handleEnemyUpdate = (enemies: EnemyNode[]) => {
  updateEnemies(enemies)
}

const handleZoneUpdate = (zones: ProtectionZone[]) => {
  updateZones(zones)
}

onMounted(() => {
  wsClient.subscribe('ENEMY_UPDATE', handleEnemyUpdate)
  wsClient.subscribe('ZONE_UPDATE', handleZoneUpdate)
})

onUnmounted(() => {
  clearEnemyMarkers()
  clearZoneLayers()
  wsClient.unsubscribe('ENEMY_UPDATE', handleEnemyUpdate)
  wsClient.unsubscribe('ZONE_UPDATE', handleZoneUpdate)
})

const updateEnemies = (enemies: EnemyNode[]) => {
  clearEnemyMarkers()

  for (const enemy of enemies) {
    const marker = createEnemy(map, [enemy.latitude, enemy.longitude])
    marker.on('contextmenu', (event: any) => {
      event.originalEvent.preventDefault()
      event.originalEvent.stopPropagation()
      L.DomEvent.stop(event.originalEvent)
      dispatchMapContextMenu({
        x: event.originalEvent.clientX,
        y: event.originalEvent.clientY,
        target: { kind: 'enemy', id: String(enemy.id), enemy },
      })
    })
    enemyMarkers.push(marker as CircleMarker)
  }
}

const updateZones = (zones: ProtectionZone[]) => {
  clearZoneLayers()

  for (const zone of zones) {
    if (!Array.isArray(zone.location) || zone.location.length < 2) {
      continue
    }

    const lat = Number(zone.location[1])
    const lng = Number(zone.location[0])
    const radius = Number(zone.size ?? 1000000)
    const { marker, circle } = createZone(map, [lat, lng], radius)

    const handleZoneContextMenu = (event: any) => {
      event.originalEvent.preventDefault()
      event.originalEvent.stopPropagation()
      L.DomEvent.stop(event.originalEvent)
      dispatchMapContextMenu({
        x: event.originalEvent.clientX,
        y: event.originalEvent.clientY,
        target: { kind: 'zone', id: String(zone.id), zone },
      })
    }

    marker.on('contextmenu', handleZoneContextMenu)
    circle.on('contextmenu', handleZoneContextMenu)

    zoneLayers.push(marker as Layer)
    zoneLayers.push(circle as Layer)
  }
}

function clearEnemyMarkers() {
  try {
    for (const marker of enemyMarkers) {
      if (marker && map) {
        map.removeLayer(marker as any)
      }
    }
  } finally {
    enemyMarkers = []
  }
}

function clearZoneLayers() {
  try {
    for (const layer of zoneLayers) {
      if (layer && map) {
        map.removeLayer(layer)
      }
    }
  } finally {
    zoneLayers = []
  }
}
</script>
