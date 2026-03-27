class WebSocketClient {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.connecting = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.listeners = {}
    this.currentUrl = ''
    this.manualClose = false
  }

  connect(url) {
    if (!url) {
      return
    }
    if ((this.isConnected || this.connecting) && this.currentUrl === url) {
      return
    }
    this.manualClose = false
    this.currentUrl = url
    const token = uni.getStorageSync('token')
    if (!token) {
      return
    }

    if (this.socketTask) {
      this.socketTask.close()
      this.socketTask = null
    }
    this.connecting = true

    this.socketTask = uni.connectSocket({
      url: `${url}?token=${token}`,
      success: () => {
        console.log('WebSocket连接成功')
      }
    })

    this.socketTask.onOpen(() => {
      this.connecting = false
      this.isConnected = true
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
      this.connecting = false
      this.isConnected = false
      console.log('WebSocket已关闭')
      this.stopHeartbeat()
      this.socketTask = null
      if (!this.manualClose) {
        this.reconnect(url)
      }
    })

    this.socketTask.onError((err) => {
      this.connecting = false
      console.error('WebSocket错误', err)
    })
  }

  send(data) {
    if (this.isConnected && this.socketTask) {
      this.socketTask.send({
        data: JSON.stringify(data)
      })
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
      if (!callback) {
        this.listeners[event] = []
        return
      }
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

  reconnect(url) {
    if (this.reconnectTimer) return

    this.reconnectTimer = setTimeout(() => {
      console.log('尝试重新连接WebSocket')
      this.connect(url)
      this.reconnectTimer = null
    }, 5000)
  }

  close() {
    this.manualClose = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socketTask) {
      this.socketTask.close()
      this.socketTask = null
    }
    this.isConnected = false
    this.connecting = false
    this.currentUrl = ''
  }
}

export default new WebSocketClient()
