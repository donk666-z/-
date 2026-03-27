class WebSocketClient {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.isConnecting = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.listeners = {}
    this.currentUrl = null
  }

  connect(url) {
    if (this.isConnecting || this.isConnected) {
      console.log('WebSocket已在连接中或已连接')
      return
    }

    const token = uni.getStorageSync('token')
    this.isConnecting = true
    this.currentUrl = url

    this.socketTask = uni.connectSocket({
      url: `${url}?token=${token}`,
      success: () => {
        console.log('WebSocket连接成功')
      },
      fail: (err) => {
        console.error('WebSocket连接失败', err)
        this.isConnecting = false
      }
    })

    this.socketTask.onOpen(() => {
      this.isConnected = true
      this.isConnecting = false
      console.log('WebSocket已打开')
      this.startHeartbeat()
    })

    this.socketTask.onMessage((res) => {
      try {
        const data = JSON.parse(res.data)
        this.handleMessage(data)
      } catch (e) {
        console.error('解析消息失败', e)
      }
    })

    this.socketTask.onClose(() => {
      this.isConnected = false
      this.isConnecting = false
      console.log('WebSocket已关闭')
      this.stopHeartbeat()
    })

    this.socketTask.onError((err) => {
      console.error('WebSocket错误', err)
      this.isConnected = false
      this.isConnecting = false
    })
  }

  send(data) {
    if (this.isConnected && this.socketTask) {
      try {
        this.socketTask.send({
          data: JSON.stringify(data),
          fail: (err) => {
            console.error('WebSocket发送失败', err)
          }
        })
      } catch (e) {
        console.error('WebSocket发送异常', e)
      }
    } else {
      console.warn('WebSocket未连接，无法发送:', data)
    }
  }

  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  }

  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
  }

  handleMessage(data) {
    const { event, payload } = data
    if (this.listeners[event]) {
      this.listeners[event].forEach(callback => callback(payload))
    }
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'heartbeat' })
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  close() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socketTask) {
      this.socketTask.close({
        success: () => {
          console.log('WebSocket已关闭')
        }
      })
      this.socketTask = null
    }
    this.isConnected = false
    this.isConnecting = false
    this.listeners = {}
  }
}

export default new WebSocketClient()
