import request from '@/utils/request'
import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import type { ApiResponse } from '@/model/response'
import type { SimulationSnapshot } from '@/model/simulation'
import type { WeaponNode } from '@/model/weaponNode'

export const simApi = {
  async toggleSimulation(action: 'start' | 'stop') {
    try {
      return await request.post<any, ApiResponse<string>>(`/sim/control/${action}`)
    } catch (error) {
      console.error('Failed to toggle simulation:', error)
      throw error
    }
  },

  async addEnemyNode(data: EnemyNode) {
    try {
      return await request.post<EnemyNode, ApiResponse<EnemyNode>>('/sim/add-enemy', data)
    } catch (error) {
      console.error('Failed to add enemy target:', error)
      throw error
    }
  },

  async updateEnemyNode(enemyId: string, data: EnemyNode) {
    try {
      return await request.put<EnemyNode, ApiResponse<EnemyNode>>(`/sim/enemy/${enemyId}`, data)
    } catch (error) {
      console.error('Failed to update enemy target:', error)
      throw error
    }
  },

  async deleteEnemyNode(enemyId: string) {
    try {
      return await request.delete<any, ApiResponse<string>>(`/sim/enemy/${enemyId}`)
    } catch (error) {
      console.error('Failed to delete enemy target:', error)
      throw error
    }
  },

  async addProtectionZone(data: ProtectionZone) {
    try {
      return await request.post<ProtectionZone, ApiResponse<ProtectionZone>>('/sim/add-zone', data)
    } catch (error) {
      console.error('Failed to add protection zone:', error)
      throw error
    }
  },

  async updateProtectionZone(zoneId: string, data: ProtectionZone) {
    try {
      return await request.put<ProtectionZone, ApiResponse<ProtectionZone>>(`/sim/zone/${zoneId}`, data)
    } catch (error) {
      console.error('Failed to update protection zone:', error)
      throw error
    }
  },

  async deleteProtectionZone(zoneId: string) {
    try {
      return await request.delete<any, ApiResponse<string>>(`/sim/zone/${zoneId}`)
    } catch (error) {
      console.error('Failed to delete protection zone:', error)
      throw error
    }
  },

  async clearAllObjects() {
    try {
      return await request.delete<any, ApiResponse<string>>('/sim/clear-all')
    } catch (error) {
      console.error('Failed to clear map objects:', error)
      throw error
    }
  },

  async getSimulationSnapshot() {
    try {
      return await request.get<any, ApiResponse<SimulationSnapshot>>('/sim/snapshot')
    } catch (error) {
      console.error('Failed to load simulation snapshot:', error)
      throw error
    }
  },

  async initWeaponNodes() {
    try {
      return await request.post<any, string>('/weapon/init-nodes')
    } catch (error) {
      console.error('Failed to init weapon nodes:', error)
      throw error
    }
  },

  async updateAllWeaponStatus(status: 0 | 1 | 2) {
    try {
      return await request.put<{ status: number }, ApiResponse<WeaponNode[]>>('/weapon/nodes/status', { status })
    } catch (error) {
      console.error('Failed to update weapon statuses:', error)
      throw error
    }
  },

  async updateWeaponStatus(weaponId: string, status: 0 | 1 | 2) {
    try {
      return await request.put<{ status: number }, ApiResponse<WeaponNode[]>>(
        `/weapon/node/${encodeURIComponent(weaponId)}/status`,
        { status },
      )
    } catch (error) {
      console.error('Failed to update weapon status:', error)
      throw error
    }
  },

  async autoAssignWeaponsToZones() {
    try {
      return await request.post<any, ProtectionZone[]>('/zone/auto-assign-weapons')
    } catch (error) {
      console.error('Failed to auto assign weapons to zones:', error)
      throw error
    }
  },
}
