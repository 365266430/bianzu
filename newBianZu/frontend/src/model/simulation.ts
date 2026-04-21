import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import type { WeaponNode } from '@/model/weaponNode'

export interface SimulationSnapshot {
  weaponNodes: WeaponNode[]
  enemyNodes: EnemyNode[]
  zones: ProtectionZone[]
}
