import request from '@/utils/request'
import type { ApiResponse } from '@/model/response'
import type {
  DynamicFormationRequest,
  DynamicFormationResult,
  StaticFormationConfig,
  StaticFormationResult,
} from '@/model/formation'

export const formationApi = {
  async generateStaticFormation(config: StaticFormationConfig) {
    return await request.post<StaticFormationConfig, ApiResponse<StaticFormationResult>>(
      '/formation/static/generate',
      config,
    )
  },

  async generateDynamicFormation(payload: DynamicFormationRequest) {
    return await request.post<DynamicFormationRequest, ApiResponse<DynamicFormationResult>>(
      '/formation/dynamic/generate',
      payload,
    )
  },

  async getParadigms() {
    return await request.get<any, ApiResponse<string[]>>('/formation/paradigms')
  },
}
