/**
 * 后端通用响应模型（对应后端的 code/message/data 结构）
 * @template T - data 字段的泛型类型，支持任意业务数据类型
 */
export interface ApiResponse<T = any> {
  // 后端的 long 类型在前端对应 number（JS/TS 中无 long，number 可覆盖整数范围）
  code: number;
  // 后端的 message 字符串
  message: string;
  // 泛型 data，默认 any，使用时指定具体类型
  data: T;
}