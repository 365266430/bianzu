import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'

export type MapContextTarget =
  | { kind: 'map' }
  | { kind: 'enemy'; id: string; enemy: EnemyNode }
  | { kind: 'zone'; id: string; zone: ProtectionZone }

export interface MapContextMenuDetail {
  x: number
  y: number
  target: MapContextTarget
}

export const MAP_CONTEXT_MENU_EVENT = 'map-context-menu'

export function dispatchMapContextMenu(detail: MapContextMenuDetail) {
  window.dispatchEvent(new CustomEvent<MapContextMenuDetail>(MAP_CONTEXT_MENU_EVENT, { detail }))
}
