import request from '@/utils/request';

// 定义控制相关的 API
export const simApi = {
    // 开始/停止仿真
    async toggleSimulation(action: 'start' | 'stop') {
        try{
            const response=await request.post<any,ApiResponse<string>>(`/sim/control/${action}`);
            console.log("仿真控制响应:",response);
            console.log("仿真控制响应数据:",response.data);
            return response;
        }catch(error){
            console.error("仿真控制错误:",error);
            throw error;
        }
    },

    async addEnemyNode(data: EnemyNode){
        try{
            const response=await request.post<EnemyNode, ApiResponse<EnemyNode>>('/sim/add-enemy',data);
            return response;
        }catch(error){
            console.error("添加敌方目标错误:",error);
            throw error;
        }
    }
};

// 你需要引入 Enemy 接口类型，或者简单的用 any
import type { EnemyNode } from '@/model/enemy';
import type { ApiResponse } from '@/model/response';
