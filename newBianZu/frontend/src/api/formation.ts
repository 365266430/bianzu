import request from '@/utils/request'
import type { ApiResponse } from '@/model/response'
import type { StaticFormationConfig, StaticFormationResult } from '@/model/formation'

export const formationApi = {
  async generateStaticFormation(config: StaticFormationConfig) {
    return await request.post<StaticFormationConfig, ApiResponse<StaticFormationResult>>(
      '/formation/static/generate',
      config,
    )
  },
}
