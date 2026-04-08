import request from '@/utils/request'
import type { EnemyNode } from '@/model/enemy'
import type { ProtectionZone } from '@/model/protectionZone'
import type { ApiResponse } from '@/model/response'

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
}
