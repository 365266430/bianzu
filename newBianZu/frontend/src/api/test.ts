import request from '@/utils/request';

// 定义控制相关的 API
export const testApi = {
    // 测试后端连接
    testConnection() {
        const response = request.get<string>('/test/connection');
        console.log("测试连接响应:", response);
        return response;
    }
};