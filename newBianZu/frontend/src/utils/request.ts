// src/utils/request.ts
import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

// 创建axios实例（AxiosInstance 作为类型标注）
const service: AxiosInstance = axios.create({
  // 基础 URL，建议在 .env 文件中配置 VITE_API_URL
  // 如果没配置，这就默认用 localhost:8080
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', 
  timeout: 5000, // 请求超时时间：5秒
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
});

// 2. 请求拦截器 (Request Interceptor)
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 在发送请求之前做些什么
    // 例如：如果有 Token，加到 Header 里
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error: any) => {
    // 对请求错误做些什么
    return Promise.reject(error);
  }
);

// 3. 响应拦截器 (Response Interceptor)
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 2xx 范围内的状态码都会触发该函数
    // 这里的 response.data 就是后端返回的真实数据
    // 如果你后端的格式是统一的 { code: 200, data: ..., msg: ... }
    // 你可以在这里解包，直接返回 response.data.data
    return response.data; 
  },
  (error: any) => {
    // 超出 2xx 范围的状态码都会触发该函数
    let message = '';
    const status = error.response?.status;
    
    switch (status) {
      case 400: message = '请求参数错误'; break;
      case 401: message = '未授权，请登录'; break;
      case 403: message = '拒绝访问'; break;
      case 404: message = '请求地址出错'; break;
      case 500: message = '服务器内部错误'; break;
      default: message = '网络连接故障';
    }

    // 这里可以换成你用的 UI 库的提示，比如 Element Plus 的 ElMessage
    console.error(`[API Error] ${message}`, error);
    // alert(message); 
    
    return Promise.reject(error);
  }
);
// 4. 导出封装后的实例
export default service;