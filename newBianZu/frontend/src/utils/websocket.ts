// 定义回调函数的类型
type MessageCallback = (data: any) => void;

class WebSocketClient {
  private url: string = '';
  private socket: WebSocket | null = null;
  private isConnected: boolean = false;
  
  // 重连机制相关
  private reconnectTimer: any = null;
  private reconnectInterval: number = 3000; // 3秒重连一次

  // 消息监听器列表 (观察者模式)
  // 比如: { 'ENEMY_UPDATE': [地图回调函数, 列表回调函数] }
  private listeners: Map<string, MessageCallback[]> = new Map();

  constructor(url: string) {
    this.url = url;
  }

  // 1. 连接方法
  public connect() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.socket = new WebSocket(this.url);

    this.socket.onopen = () => {
      console.log(`WS 已连接: ${this.url}`);
      this.isConnected = true;
      // 可以在这里发心跳包
    };

    this.socket.onclose = () => {
      console.log('WS 连接断开');
      this.isConnected = false;
      this.handleReconnect();
    };

    this.socket.onerror = (err) => {
      console.error('WS 发生错误', err);
      this.isConnected = false;
    };

    // 核心：收到消息后的分发逻辑
    this.socket.onmessage = (event) => {
      try {
        // 假设后端传回来的是我们设计的信封结构: { type: 'ENEMY_UPDATE', payload: [...] }
        const message = JSON.parse(event.data);
        const { type, payload } = message;

        // 找到所有订阅了这个 type 的回调函数，挨个执行
        if (this.listeners.has(type)) {
          this.listeners.get(type)?.forEach(callback => callback(payload));
        } else {
          // 如果没有特定 type，或者是纯数据，可以触发默认回调 (可选)
          // console.log("收到未分类消息:", message);
        }

      } catch (e) {
        console.error("WS 消息解析失败", e);
      }
    };
  }

  // 2. 断线重连逻辑
  private handleReconnect() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);

    this.reconnectTimer = setTimeout(() => {
      console.log('WS 尝试重连...');
      this.connect();
    }, this.reconnectInterval);
  }

  // 3. 订阅消息 (组件调用这个方法)
  // 例如: wsClient.subscribe('ENEMY_UPDATE', (enemies) => { 更新地图 })
  public subscribe(type: string, callback: MessageCallback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, []);
    }
    this.listeners.get(type)?.push(callback);
  }

  // 4. 取消订阅 (组件销毁时调用)
  public unsubscribe(type: string, callback: MessageCallback) {
    const callbacks = this.listeners.get(type);
    if (callbacks) {
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  // 5. 主动关闭
  public close() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}

// 导出单例实例
// 这里的 URL 可以从环境变量读
const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/sim';
export const wsClient = new WebSocketClient(wsUrl);