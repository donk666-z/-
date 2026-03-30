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
      this.isConnected = true
      this.connecting = false
      this.startHeartbeat()
      this.emit('socketOpen')
    })

    this.socketTask.onMessage((res) => {
      try {
        const data = JSON.parse(res.data)
        this.handleMessage(data)
      } catch (error) {
        console.error('解析 WebSocket 消息失败', error)
      }
    })

    this.socketTask.onClose(() => {
      this.isConnected = false
      this.connecting = false
      this.stopHeartbeat()
      this.socketTask = null
      this.emit('socketClose')
      if (!this.manualClose) {
        this.reconnect(url)
      }
    })

    this.socketTask.onError((error) => {
      this.isConnected = false
      this.connecting = false
      console.error('WebSocket 连接异常', error)
    })
  }

  send(data) {
    if (!this.isConnected || !this.socketTask) {
      return
    }
    this.socketTask.send({
      data: JSON.stringify(data)
    })
  }

  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  }

  off(event, callback) {
    if (!this.listeners[event]) {
      return
    }
    if (!callback) {
      this.listeners[event] = []
      return
    }
    this.listeners[event] = this.listeners[event].filter((current) => current !== callback)
  }

  emit(event, payload) {
    if (this.listeners[event]) {
      this.listeners[event].forEach((callback) => callback(payload))
    }
  }

  handleMessage(data) {
    const { event, payload } = data
    this.emit(event, payload)
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

  reconnect(url) {
    if (this.reconnectTimer) {
      return
    }
    this.reconnectTimer = setTimeout(() => {
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
