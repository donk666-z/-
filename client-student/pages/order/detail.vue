<template>
  <view class="container">
    <view class="hero-card">
      <text class="status-badge">{{ getStatusText(order.status) }}</text>
      <text class="status-text">{{ getStatusHeadline(order.status) }}</text>
      <text class="status-desc">{{ getStatusDescription(order.status) }}</text>
      <text class="status-desc eta" v-if="order.estimatedTime">送达预估 {{ order.estimatedTime }}</text>
    </view>

    <view class="card">
      <view class="section-head">
        <text class="section-title">配送进度</text>
        <text class="section-tip">{{ activeStep.label }}</text>
      </view>
      <view class="progress-strip">
        <view v-for="step in statusSteps" :key="step.key" class="progress-item">
          <view :class="['progress-dot', step.state]"></view>
          <text :class="['progress-label', step.state]">{{ step.label }}</text>
        </view>
      </view>
    </view>

    <view class="card" v-if="hasMapData">
      <view class="section-head">
        <text class="section-title">实时地图</text>
        <text class="section-tip">{{ mapSummary }}</text>
      </view>
      <map
        class="delivery-map"
        :latitude="mapCenter.latitude"
        :longitude="mapCenter.longitude"
        :scale="mapScale"
        :markers="mapMarkers"
        :polyline="mapPolylines"
        :include-points="mapIncludePoints"
        :show-location="false"
        :enable-scroll="true"
        :enable-zoom="true"
      ></map>
      <view class="map-legend">
        <view v-for="legend in mapLegends" :key="legend.key" class="legend-item">
          <text class="legend-dot" :style="{ background: legend.color }"></text>
          <text class="legend-text">{{ legend.label }}</text>
        </view>
      </view>
    </view>

    <view class="card card-muted" v-else>
      <view class="section-head">
        <text class="section-title">实时地图</text>
      </view>
      <text class="empty-text">当前订单还没有可展示的坐标信息，骑手开始配送后会自动显示实时位置。</text>
    </view>

    <view class="card" v-if="order.riderName && ['delivering', 'delivered', 'completed'].includes(order.status)">
      <view class="section-head">
        <text class="section-title">骑手信息</text>
        <text class="link" v-if="order.riderPhone" @click="callRider">联系骑手</text>
      </view>
      <view class="rider-row">
        <view>
          <text class="rider-name">{{ order.riderName }}</text>
          <text class="rider-meta">{{ order.riderPhone || '暂未提供联系电话' }}</text>
        </view>
        <text class="rider-tag">{{ order.status === 'delivering' ? '配送中' : '已完成配送' }}</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">配送信息</text>
      <view class="info-row"><text class="label">收货地址</text><text class="value-text">{{ order.address }}</text></view>
      <view class="info-row"><text class="label">联系电话</text><text>{{ order.phone }}</text></view>
      <view class="info-row" v-if="order.merchantName"><text class="label">商户名称</text><text>{{ order.merchantName }}</text></view>
      <view class="info-row" v-if="order.orderNo"><text class="label">订单编号</text><text>{{ order.orderNo }}</text></view>
      <view class="info-row" v-if="order.createdAt"><text class="label">下单时间</text><text>{{ order.createdAt }}</text></view>
    </view>

    <view class="card">
      <text class="section-title">{{ order.merchantName || '商品明细' }}</text>
      <view v-for="(dish, index) in order.dishes" :key="`${dish.id || 'dish'}-${index}`" class="dish-item">
        <view class="dish-main">
          <view class="dish-head">
            <text class="dish-name">{{ dish.name }}</text>
            <text v-if="dish.type === 'combo'" class="dish-tag">套餐</text>
          </view>
          <view v-if="dish.type === 'combo' && Array.isArray(dish.comboGroups)" class="combo-lines">
            <text v-for="line in getComboLines(dish.comboGroups)" :key="line" class="combo-line">{{ line }}</text>
          </view>
        </view>
        <text class="dish-quantity">x{{ dish.quantity }}</text>
        <text class="dish-price">¥{{ dish.price }}</text>
      </view>
    </view>

    <view class="card">
      <view class="info-row"><text>菜品总价</text><text>¥{{ order.dishPrice }}</text></view>
      <view class="info-row"><text>配送费</text><text>¥{{ order.deliveryFee }}</text></view>
      <view class="info-row total"><text>实付金额</text><text class="total-price">¥{{ order.totalPrice }}</text></view>
    </view>

    <view class="action-bar" v-if="showActions">
      <button v-if="['pending', 'paid'].includes(order.status)" size="mini" @click="handleCancel">取消订单</button>
      <button v-if="order.status === 'delivered'" type="primary" size="mini" @click="handleConfirm">确认收货</button>
      <button v-if="order.status === 'completed' && !order.reviewed" type="warn" size="mini" @click="goReview">去评价</button>
      <button v-if="order.status === 'completed'" size="mini" @click="reorder">再来一单</button>
    </view>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { getOrderDetail, getOrderRoute, cancelOrder, confirmOrder } from '@/api/order'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import websocket from '@/utils/websocket'
import { BASE_URL } from '@/utils/request'

const STATUS_TEXT_MAP = {
  pending: '待支付',
  paid: '待接单',
  accepted: '制作中',
  prepared: '待取餐',
  delivering: '配送中',
  delivered: '已送达',
  completed: '已完成',
  cancelled: '已取消'
}

const STATUS_HEADLINE_MAP = {
  pending: '订单等待支付',
  paid: '商户待接单',
  accepted: '商家正在制作',
  prepared: '骑手待取餐',
  delivering: '骑手正在配送',
  delivered: '餐品已送达',
  completed: '订单已完成',
  cancelled: '订单已取消'
}

const STATUS_DESCRIPTION_MAP = {
  pending: '支付完成后，商户会第一时间处理你的订单。',
  paid: '订单已提交给商户，接单后会进入制作流程。',
  accepted: '商户已经接单，正在备餐，请稍等片刻。',
  prepared: '餐品已完成，等待骑手取餐并开始配送。',
  delivering: '你可以在地图上实时查看骑手位置和配送进度。',
  delivered: '餐品已送达，请及时确认收货。',
  completed: '这笔订单已经完成，欢迎再次下单。',
  cancelled: '订单已取消，如有需要可以重新下单。'
}

const STEP_META = [
  { key: 'paid', label: '接单' },
  { key: 'accepted', label: '制作中' },
  { key: 'delivering', label: '配送中' },
  { key: 'delivered', label: '已送达' }
]

const MARKER_ICONS = {
  merchant: '/static/map-merchant.png',
  destination: '/static/map-destination.png',
  rider: '/static/map-rider.png'
}

const toNumber = (value) => {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

const toPoint = (latitude, longitude) => {
  const lat = toNumber(latitude)
  const lng = toNumber(longitude)
  if (lat === null || lng === null) {
    return null
  }
  return {
    latitude: lat,
    longitude: lng
  }
}

const midpoint = (pointA, pointB) => {
  if (pointA && pointB) {
    return {
      latitude: Number(((pointA.latitude + pointB.latitude) / 2).toFixed(6)),
      longitude: Number(((pointA.longitude + pointB.longitude) / 2).toFixed(6))
    }
  }
  return pointA || pointB || { latitude: 0, longitude: 0 }
}

const normalizeOrder = (order, previousOrder = {}) => {
  const next = {
    ...(order || {})
  }
  const keepRealtimeRider = next.status === 'delivering'
  next.dishes = Array.isArray(next.dishes) ? next.dishes : []
  next.riderLat = next.riderLat != null ? Number(next.riderLat) : (keepRealtimeRider ? previousOrder.riderLat : null)
  next.riderLng = next.riderLng != null ? Number(next.riderLng) : (keepRealtimeRider ? previousOrder.riderLng : null)
  next.addressLat = next.addressLat != null ? Number(next.addressLat) : previousOrder.addressLat
  next.addressLng = next.addressLng != null ? Number(next.addressLng) : previousOrder.addressLng
  next.merchantLat = next.merchantLat != null ? Number(next.merchantLat) : previousOrder.merchantLat
  next.merchantLng = next.merchantLng != null ? Number(next.merchantLng) : previousOrder.merchantLng
  next.estimatedTime = next.estimatedTime || previousOrder.estimatedTime || ''
  return next
}

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      orderId: '',
      order: {
        dishes: []
      },
      routePlan: null,
      lastRouteRefreshAt: 0,
      orderStatusHandler: null,
      riderLocationHandler: null,
      socketOpenHandler: null,
      subscribeTimer: null
    }
  },
  computed: {
    showActions() {
      return ['pending', 'paid', 'delivered', 'completed'].includes(this.order.status)
    },
    merchantPoint() {
      return toPoint(this.order.merchantLat, this.order.merchantLng)
    },
    addressPoint() {
      return toPoint(this.order.addressLat, this.order.addressLng)
    },
    riderPoint() {
      if (this.order.status !== 'delivering') {
        return null
      }
      return toPoint(this.order.riderLat, this.order.riderLng)
    },
    routePoints() {
      const points = Array.isArray(this.routePlan && this.routePlan.points) ? this.routePlan.points : []
      return points
        .map((point) => toPoint(point.latitude, point.longitude))
        .filter(Boolean)
    },
    hasMapData() {
      return this.routePoints.length > 1 || !!(this.addressPoint || this.merchantPoint || this.riderPoint)
    },
    activeStep() {
      if (this.order.status === 'prepared') {
        return { key: 'prepared', label: '待取餐' }
      }
      return STEP_META.find((step) => step.key === (this.order.status === 'completed' ? 'delivered' : this.order.status)) || {
        key: this.order.status || 'unknown',
        label: this.getStatusText(this.order.status)
      }
    },
    statusSteps() {
      const status = this.order.status
      return STEP_META.map((step) => {
        let state = 'pending'
        if (['completed'].includes(status)) {
          state = 'done'
        } else if (status === 'prepared') {
          if (step.key === 'paid' || step.key === 'accepted') {
            state = step.key === 'accepted' ? 'active' : 'done'
          }
        } else if (status === step.key) {
          state = 'active'
        } else if (STEP_META.findIndex((item) => item.key === status) > STEP_META.findIndex((item) => item.key === step.key)) {
          state = 'done'
        }
        return {
          ...step,
          state
        }
      })
    },
    mapCenter() {
      if (this.routePoints.length > 1) {
        return midpoint(this.routePoints[0], this.routePoints[this.routePoints.length - 1])
      }
      if (this.riderPoint && this.addressPoint) {
        return midpoint(this.riderPoint, this.addressPoint)
      }
      if (this.merchantPoint && this.addressPoint) {
        return midpoint(this.merchantPoint, this.addressPoint)
      }
      return this.riderPoint || this.addressPoint || this.merchantPoint || { latitude: 0, longitude: 0 }
    },
    mapScale() {
      return this.riderPoint && this.addressPoint ? 14 : 15
    },
    mapIncludePoints() {
      return [this.merchantPoint, this.addressPoint, this.riderPoint].filter(Boolean)
    },
    mapMarkers() {
      const markers = []

      if (this.merchantPoint) {
        markers.push(this.buildMarker(1, 'merchant', this.merchantPoint, this.order.merchantName || '商户'))
      }
      if (this.addressPoint) {
        markers.push(this.buildMarker(2, 'destination', this.addressPoint, '收货点'))
      }
      if (this.riderPoint) {
        markers.push(this.buildMarker(3, 'rider', this.riderPoint, this.order.riderName || '骑手'))
      }

      return markers
    },
    mapPolylines() {
      if (this.routePoints.length > 1) {
        return [
          {
            points: this.routePoints,
            color: '#FF6B00',
            width: 8,
            arrowLine: this.order.status === 'delivering'
          }
        ]
      }
      const polylines = []
      if (this.merchantPoint && this.addressPoint) {
        polylines.push({
          points: [this.merchantPoint, this.addressPoint],
          color: '#FFD1AE',
          width: 6,
          dottedLine: true
        })
      }
      if (this.riderPoint && this.addressPoint) {
        polylines.push({
          points: [this.riderPoint, this.addressPoint],
          color: '#FF6B00',
          width: 8,
          arrowLine: true
        })
      }
      return polylines
    },
    mapSummary() {
      if (this.routePlan && this.routePlan.etaText) {
        return this.routePlan.etaText
      }
      if (this.order.status === 'delivering' && this.order.estimatedTime) {
        return `送达预估 ${this.order.estimatedTime}`
      }
      if (this.order.status === 'prepared') {
        return '骑手待取餐'
      }
      if (this.order.status === 'accepted') {
        return '商家制作中'
      }
      return this.getStatusText(this.order.status)
    },
    mapLegends() {
      const legends = []
      if (this.merchantPoint) {
        legends.push({ key: 'merchant', label: '商户', color: '#ffb347' })
      }
      if (this.addressPoint) {
        legends.push({ key: 'destination', label: '收货点', color: '#4c87ff' })
      }
      if (this.riderPoint) {
        legends.push({ key: 'rider', label: '骑手', color: '#19be6b' })
      }
      return legends
    }
  },
  onLoad(options) {
    this.orderId = options.id
    this.loadOrder()
    this.setupRealtime()
  },
  onShow() {
    if (this.orderId) {
      this.loadOrder()
      this.subscribeOrder()
    }
  },
  onUnload() {
    this.teardownRealtime()
  },
  methods: {
    normalizeRoute(route) {
      if (!route) {
        return null
      }
      return {
        ...route,
        smart: !!route.smart,
        points: Array.isArray(route.points) ? route.points : []
      }
    },
    shouldRequestRoute() {
      return ['accepted', 'prepared', 'delivering'].includes(this.order.status) && !!this.addressPoint
    },
    async loadRoute(force = false) {
      if (!this.shouldRequestRoute()) {
        this.routePlan = null
        return
      }
      if (!force && Date.now() - this.lastRouteRefreshAt < 55000) {
        return
      }
      try {
        const route = await getOrderRoute(this.orderId)
        this.routePlan = this.normalizeRoute(route)
        this.lastRouteRefreshAt = Date.now()
      } catch (error) {
        console.error(error)
      }
    },
    async loadOrder() {
      try {
        const latest = await getOrderDetail(this.orderId)
        this.order = normalizeOrder(latest, this.order)
        await this.loadRoute(true)
      } catch (error) {
        console.error(error)
      }
    },
    resolveWsUrl() {
      return `${BASE_URL.replace(/^http/i, 'ws')}/ws`
    },
    setupRealtime() {
      if (!this.orderId) {
        return
      }

      this.socketOpenHandler = () => {
        this.subscribeOrder()
      }
      this.orderStatusHandler = (payload = {}) => {
        if (String(payload.orderId) !== String(this.orderId)) {
          return
        }
        const previousStatus = this.order.status
        this.order = normalizeOrder(
          {
            ...this.order,
            status: payload.status || this.order.status,
            estimatedTime: payload.estimatedTime || this.order.estimatedTime
          },
          this.order
        )
        if (payload.status === 'delivered' && previousStatus !== 'delivered' && previousStatus !== 'completed') {
          uni.showToast({
            title: '订单已送达，请及时确认收货',
            icon: 'none',
            duration: 2200
          })
        }
        this.loadRoute(true)
        setTimeout(() => {
          this.loadOrder()
        }, 300)
      }
      this.riderLocationHandler = (payload = {}) => {
        if (String(payload.orderId) !== String(this.orderId)) {
          return
        }
        this.order = normalizeOrder(
          {
            ...this.order,
            riderLat: payload.lat,
            riderLng: payload.lng
          },
          this.order
        )
        this.loadRoute()
      }

      websocket.on('socketOpen', this.socketOpenHandler)
      websocket.on('orderStatusUpdate', this.orderStatusHandler)
      websocket.on('riderLocation', this.riderLocationHandler)
      websocket.connect(this.resolveWsUrl())
      this.subscribeOrder()
    },
    subscribeOrder() {
      if (!this.orderId) {
        return
      }
      if (this.subscribeTimer) {
        clearTimeout(this.subscribeTimer)
        this.subscribeTimer = null
      }
      if (websocket.isConnected) {
        websocket.send({
          type: 'subscribe',
          orderId: Number(this.orderId)
        })
        return
      }
      this.subscribeTimer = setTimeout(() => {
        this.subscribeOrder()
      }, 400)
    },
    teardownRealtime() {
      if (this.subscribeTimer) {
        clearTimeout(this.subscribeTimer)
        this.subscribeTimer = null
      }
      if (this.orderId && websocket.isConnected) {
        websocket.send({
          type: 'unsubscribe',
          orderId: Number(this.orderId)
        })
      }
      if (this.socketOpenHandler) {
        websocket.off('socketOpen', this.socketOpenHandler)
        this.socketOpenHandler = null
      }
      if (this.orderStatusHandler) {
        websocket.off('orderStatusUpdate', this.orderStatusHandler)
        this.orderStatusHandler = null
      }
      if (this.riderLocationHandler) {
        websocket.off('riderLocation', this.riderLocationHandler)
        this.riderLocationHandler = null
      }
      websocket.close()
    },
    buildMarker(id, type, point, label) {
      return {
        id,
        latitude: point.latitude,
        longitude: point.longitude,
        width: 36,
        height: 36,
        iconPath: MARKER_ICONS[type],
        callout: {
          content: label,
          color: '#1f2937',
          bgColor: '#ffffff',
          borderRadius: 14,
          borderWidth: 1,
          borderColor: '#eef1f5',
          padding: 8,
          display: 'ALWAYS',
          fontSize: 12
        }
      }
    },
    getComboLines(comboGroups) {
      return (Array.isArray(comboGroups) ? comboGroups : [])
        .filter((group) => Array.isArray(group.options) && group.options.length > 0)
        .map((group) => `${group.name || '已选'}：${group.options.map((option) => `${option.dishName} x${option.totalQuantity || option.quantity || 1}`).join('、')}`)
    },
    callRider() {
      if (this.order.riderPhone) {
        uni.makePhoneCall({ phoneNumber: this.order.riderPhone })
      }
    },
    async handleCancel() {
      uni.showModal({
        title: '提示',
        content: '确定取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelOrder(this.orderId)
              uni.showToast({ title: '已取消', icon: 'none' })
              this.loadOrder()
            } catch (error) {
              console.error(error)
            }
          }
        }
      })
    },
    async handleConfirm() {
      try {
        await confirmOrder(this.orderId)
        uni.showToast({ title: '已确认收货', icon: 'none' })
        this.loadOrder()
      } catch (error) {
        console.error(error)
      }
    },
    goReview() {
      uni.navigateTo({ url: `/pages/order/review?orderId=${this.orderId}` })
    },
    reorder() {
      uni.navigateTo({ url: `/pages/merchant/detail?id=${this.order.merchantId}` })
    },
    getStatusText(status) {
      return STATUS_TEXT_MAP[status] || status || '订单处理中'
    },
    getStatusHeadline(status) {
      return STATUS_HEADLINE_MAP[status] || '订单处理中'
    },
    getStatusDescription(status) {
      return STATUS_DESCRIPTION_MAP[status] || '订单状态正在更新，请稍后查看。'
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 20rpx 20rpx 140rpx;
  background:
    linear-gradient(180deg, #fff5eb 0rpx, #fff5eb 240rpx, #f7f8fa 240rpx, #f7f8fa 100%);
}

.hero-card,
.card {
  border-radius: 24rpx;
  background: #fff;
}

.hero-card {
  margin-bottom: 20rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #fff2e4 0%, #ffffff 72%);
  box-shadow: 0 18rpx 40rpx rgba(255, 107, 0, 0.08);
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  box-shadow: 0 12rpx 28rpx rgba(25, 34, 53, 0.04);
}

.card-muted {
  background: #fbfcfd;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 107, 0, 0.12);
  color: #ff6b00;
  font-size: 22rpx;
  font-weight: 700;
}

.status-text {
  display: block;
  margin-top: 18rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
}

.status-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #6b7280;
}

.status-desc.eta {
  color: #ff6b00;
  font-weight: 600;
}

.section-head,
.rider-row,
.dish-head,
.info-row,
.dish-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.section-tip {
  font-size: 24rpx;
  color: #8a8f99;
}

.progress-strip {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 22rpx;
}

.progress-item {
  flex: 1;
  text-align: center;
}

.progress-dot {
  width: 22rpx;
  height: 22rpx;
  margin: 0 auto;
  border-radius: 50%;
  background: #d7dce3;
  box-shadow: 0 0 0 10rpx rgba(215, 220, 227, 0.24);
}

.progress-dot.done {
  background: #19be6b;
  box-shadow: 0 0 0 10rpx rgba(25, 190, 107, 0.18);
}

.progress-dot.active {
  background: #ff6b00;
  box-shadow: 0 0 0 10rpx rgba(255, 107, 0, 0.18);
}

.progress-label {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: #9aa3af;
}

.progress-label.done,
.progress-label.active {
  color: #1f2937;
  font-weight: 600;
}

.delivery-map {
  width: 100%;
  height: 360rpx;
  margin-top: 20rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-top: 18rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.legend-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}

.legend-text {
  font-size: 22rpx;
  color: #7b8495;
}

.empty-text {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #8a8f99;
}

.rider-name {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.rider-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a8f99;
}

.rider-tag {
  flex-shrink: 0;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #fff1e6;
  color: #ff6b00;
  font-size: 22rpx;
  font-weight: 700;
}

.info-row {
  padding: 12rpx 0;
}

.label {
  width: 160rpx;
  flex-shrink: 0;
  color: #9aa3af;
}

.value-text {
  text-align: right;
  line-height: 1.6;
}

.link {
  color: #ff6b00;
  font-size: 24rpx;
  font-weight: 600;
}

.dish-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f6;
}

.dish-item:last-child {
  border-bottom: none;
}

.dish-main {
  flex: 1;
  min-width: 0;
}

.dish-name {
  font-size: 28rpx;
  color: #1f2937;
}

.dish-tag {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: #fff1e6;
  color: #ff7a1a;
  font-size: 20rpx;
}

.combo-lines {
  margin-top: 8rpx;
}

.combo-line {
  display: block;
  font-size: 22rpx;
  line-height: 1.6;
  color: #8a8f99;
}

.dish-quantity {
  color: #9aa3af;
}

.dish-price,
.total-price {
  color: #ff6b00;
  font-weight: 700;
}

.total {
  padding-top: 14rpx;
  border-top: 1rpx solid #f0f0f0;
  font-size: 30rpx;
  font-weight: 700;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  z-index: 1101;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 0 30rpx;
  height: 100rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -6rpx 24rpx rgba(17, 24, 39, 0.08);
}
</style>
