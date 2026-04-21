type MessageCallback = (data: any) => void

class WebSocketClient {
  private url = ''
  private socket: WebSocket | null = null
  private isConnected = false
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectInterval = 3000
  private listeners: Map<string, MessageCallback[]> = new Map()
  private latestPayloads: Map<string, any> = new Map()

  constructor(url: string) {
    this.url = url
  }

  public connect() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return
    }

    this.socket = new WebSocket(this.url)

    this.socket.onopen = () => {
      console.log(`WS connected: ${this.url}`)
      this.isConnected = true
    }

    this.socket.onclose = () => {
      console.log('WS disconnected')
      this.isConnected = false
      this.latestPayloads.clear()
      this.handleReconnect()
    }

    this.socket.onerror = (err) => {
      console.error('WS error', err)
      this.isConnected = false
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        const { type, payload } = message ?? {}
        if (!type) {
          return
        }

        this.latestPayloads.set(type, payload)
        if (this.listeners.has(type)) {
          this.listeners.get(type)?.forEach(callback => callback(payload))
        }
      } catch (e) {
        console.error('WS message parse failed', e)
      }
    }
  }

  private handleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    this.reconnectTimer = setTimeout(() => {
      console.log('WS reconnecting...')
      this.connect()
    }, this.reconnectInterval)
  }

  public subscribe(type: string, callback: MessageCallback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, [])
    }
    this.listeners.get(type)?.push(callback)
  }

  public unsubscribe(type: string, callback: MessageCallback) {
    const callbacks = this.listeners.get(type)
    if (!callbacks) {
      return
    }

    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  public close() {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.latestPayloads.clear()
  }

  public getLatest<T = any>(type: string): T | null {
    if (!this.latestPayloads.has(type)) {
      return null
    }
    return this.latestPayloads.get(type) as T
  }
}

const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/sim'
export const wsClient = new WebSocketClient(wsUrl)
