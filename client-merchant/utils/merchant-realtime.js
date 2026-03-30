import { API_HOST } from '@/config/api'
import websocket from '@/utils/websocket'

class MerchantRealtime {
  constructor() {
    this.audioCtx = null
    this.started = false
    this.newOrderHandler = null
  }

  start(options = {}) {
    const token = uni.getStorageSync('token')
    if (!token) {
      this.stop()
      return
    }

    const { onNewOrder } = options
    const wsUrl = this.resolveWsUrl()
    if (!this.audioCtx && typeof uni.createInnerAudioContext === 'function') {
      this.audioCtx = uni.createInnerAudioContext()
      this.audioCtx.src = '/static/new-order.wav'
      this.audioCtx.obeyMuteSwitch = false
    }

    if (this.newOrderHandler) {
      websocket.off('newOrder', this.newOrderHandler)
    }
    this.newOrderHandler = (payload = {}) => {
      this.playAlert()
      uni.$emit('merchant:new-order', payload)
      if (typeof onNewOrder === 'function') {
        onNewOrder(payload)
      }
    }

    websocket.on('newOrder', this.newOrderHandler)
    websocket.connect(wsUrl)
    this.started = true
  }

  stop() {
    if (this.newOrderHandler) {
      websocket.off('newOrder', this.newOrderHandler)
      this.newOrderHandler = null
    }
    websocket.close()
    if (this.audioCtx) {
      this.audioCtx.destroy()
      this.audioCtx = null
    }
    this.started = false
  }

  playAlert() {
    if (this.audioCtx) {
      try {
        this.audioCtx.stop()
        this.audioCtx.seek(0)
        this.audioCtx.play()
      } catch (error) {
        console.error('播放新订单提醒失败', error)
      }
    }
    if (typeof uni.vibrateLong === 'function') {
      uni.vibrateLong()
    }
  }

  resolveWsUrl() {
    return `${API_HOST.replace(/^http/i, 'ws')}/api/ws`
  }
}

export default new MerchantRealtime()
