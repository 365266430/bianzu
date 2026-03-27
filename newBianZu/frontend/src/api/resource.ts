import type { FireType } from '@/model/fireType';
import type { WeaponType } from '@/model/weaponType';
import type { EnemyType } from '@/model/enemyType';
import request from '@/utils/request';
import type { ApiResponse } from '@/model/response';

// 定义武器相关的 API
export const resApi = {
    // 获取武器类型列表
    async getWeaponTypes() {
        try{
            const response=await request.get<any, ApiResponse<WeaponType[]>>('/weapon/list-types');
            return response.data;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },
    // 获取火力资源列表
    async getFireTypes() {
        try{
            const response=await request.get<any, ApiResponse<FireType[]>>('/fire/list-types');
            return response.data;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },
    // 获取敌人类型列表
    async getEnemyTypes() {
        try{
            console.log("请求敌人类型列表...");
            const response=await request.get<any, ApiResponse<EnemyType[]>>('/enemy/list-types');
            return response.data;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },

    async addEnemyType(data: EnemyType){
        try{
            const response=await request.post<EnemyType, ApiResponse<EnemyType>>('/enemy/add-type',data);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },

    async addWeaponType(data: WeaponType){
        try{
            const response=await request.post<WeaponType, ApiResponse<WeaponType>>('/weapon/add-type',data);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },

    async addFireType(data: FireType){
        try{
            const response=await request.post<FireType, ApiResponse<FireType>>('/fire/add-type',data);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },

    async deleteEnemyType(type: string){
        try{
            const response=await request.delete<any, ApiResponse<null>>(`/enemy/delete-type/${type}`);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },
    async deleteFireType(type: string){
        try{
            const response=await request.delete<any, ApiResponse<null>>(`/fire/delete-type/${type}`);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    },
    async deleteWeaponType(type: string){
        try{
            const response=await request.delete<any, ApiResponse<null>>(`/weapon/delete-type/${type}`);
            return response;
        }catch(error){
            console.error("请求错误:",error);
            throw error;
        }
    }
};

