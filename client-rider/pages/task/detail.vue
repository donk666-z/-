<template>
  <view class="page">
    <view v-if="task.id">
      <view class="hero-card">
        <view class="hero-head">
          <u-tag :text="statusText" :type="statusType" size="mini"></u-tag>
          <text class="fee">配送费 ¥{{ formatMoney(task.deliveryFee) }}</text>
        </view>
        <text class="hero-title">{{ heroTitle }}</text>
        <text class="hero-desc">{{ heroDescription }}</text>
      </view>

      <view class="card" v-if="routePlan">
        <view class="section-head">
          <view>
            <text class="section-title">智能路线</text>
            <text class="section-tip">{{ routeTip }}</text>
          </view>
          <text class="refresh-link" @click="handleRefreshRoute">
            {{ routeLoading ? '刷新中...' : '刷新路线' }}
          </text>
        </view>

        <view class="stats-grid">
          <view class="stat-card">
            <text class="stat-label">预计时长</text>
            <text class="stat-value">{{ routePlan.etaText || '--' }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">路线距离</text>
            <text class="stat-value">{{ distanceText }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-label">路线模式</text>
            <text class="stat-value">{{ routeModeText }}</text>
          </view>
        </view>

        <view class="route-line">
          <view class="route-node">
            <text class="route-dot start"></text>
            <view class="route-copy">
              <text class="route-name">{{ displaySourceLabel }}</text>
              <text class="route-sub">{{ routeStartHint }}</text>
            </view>
          </view>
          <view class="route-node">
            <text class="route-dot end"></text>
            <view class="route-copy">
              <text class="route-name">{{ routePlan.destinationLabel || task.deliveryAddress }}</text>
              <text class="route-sub">终点</text>
            </view>
          </view>
        </view>

        <view :class="['route-note', routePlan.smart ? 'smart' : 'fallback']">
          {{ routeNoteText }}
        </view>

        <map
          v-if="hasMapData"
          class="delivery-map"
          :latitude="mapCenter.latitude"
          :longitude="mapCenter.longitude"
          :scale="mapScale"
          :markers="mapMarkers"
          :polyline="mapPolyline"
          :include-points="mapIncludePoints"
          :show-location="false"
          :enable-scroll="true"
          :enable-zoom="true"
        ></map>

        <view class="map-legend" v-if="hasMapData">
          <view v-for="legend in legends" :key="legend.key" class="legend-item">
            <text class="legend-dot" :style="{ background: legend.color }"></text>
            <text class="legend-text">{{ legend.label }}</text>
          </view>
        </view>

        <view class="steps-box" v-if="routeSteps.length">
          <view class="steps-head">
            <text class="section-title small">配送步骤</text>
            <text class="steps-tip">显示前 {{ routeSteps.length }} 步</text>
          </view>
          <view v-for="(step, index) in routeSteps" :key="`${index}-${step.instruction}`" class="step-item">
            <text class="step-index">{{ index + 1 }}</text>
            <view class="step-main">
              <text class="step-text">{{ step.instruction }}</text>
              <text class="step-meta" v-if="step.distanceMeters || step.roadName">
                {{ formatStepMeta(step) }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <view class="card">
        <text class="section-title">商户信息</text>
        <view class="info-row">
          <u-icon name="shop" size="28"></u-icon>
          <text>{{ task.merchantName }}</text>
        </view>
        <view class="info-row" @click="openLocation(task.merchantLatitude, task.merchantLongitude, task.merchantAddress)">
          <u-icon name="map" size="28" color="#1f7ae0"></u-icon>
          <text class="link">{{ task.merchantAddress }}</text>
        </view>
      </view>

      <view class="card">
        <text class="section-title">配送信息</text>
        <view class="info-row">
          <u-icon name="account" size="28"></u-icon>
          <text>{{ task.customerName || '收货人' }}</text>
        </view>
        <view class="info-row" @click="callPhone(task.customerPhone)">
          <u-icon name="phone" size="28" color="#1f7ae0"></u-icon>
          <text class="link">{{ task.customerPhone || '暂无联系电话' }}</text>
        </view>
        <view class="info-row" @click="openLocation(task.deliveryLatitude, task.deliveryLongitude, task.deliveryAddress)">
          <u-icon name="map-fill" size="28" color="#1f7ae0"></u-icon>
          <text class="link">{{ task.deliveryAddress }}</text>
        </view>
      </view>

      <view class="card">
        <text class="section-title">订单信息</text>
        <view class="info-row">
          <u-icon name="file-text" size="28"></u-icon>
          <text>订单号：{{ task.orderNo }}</text>
        </view>
        <view class="info-row" v-if="task.estimatedTime">
          <u-icon name="clock" size="28"></u-icon>
          <text>系统预计：{{ task.estimatedTime }}</text>
        </view>
        <view class="info-row" v-if="task.remark">
          <u-icon name="bookmark" size="28"></u-icon>
          <text>{{ task.remark }}</text>
        </view>
      </view>

      <view class="card notice-card" v-if="waitingPrepare">
        <text class="section-title">当前进度</text>
        <text class="notice-text">你已抢到该订单。商户出餐后页面会自动切换为“待取餐”，现在可以先熟悉推荐送餐路线。</text>
      </view>
    </view>

    <u-empty v-else text="未找到任务信息" mode="list"></u-empty>

    <view class="action-bar" v-if="task.id && (canPickup || canDeliver)">
      <u-button type="primary" @click="handleAction" :loading="loading">
        {{ canPickup ? '确认已取餐' : '确认送达' }}
      </u-button>
    </view>
  </view>
</template>

<script>
import { deliverTask, getTaskDetail, getTaskRoute, pickupTask } from '@/api/task'
import { updateLocation } from '@/api/location'

const MARKER_ICONS = {
  merchant: '/static/map-merchant.png',
  destination: '/static/map-destination.png',
  rider: '/static/map-rider.png'
}

const toNumber = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

const toPoint = (latitude, longitude) => {
  const lat = toNumber(latitude)
  const lng = toNumber(longitude)
  if (lat === null || lng === null) {
    return null
  }
  return {
    latitude: Number(lat.toFixed(6)),
    longitude: Number(lng.toFixed(6))
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

export default {
  data() {
    return {
      taskId: '',
      task: {},
      routePlan: null,
      currentPosition: null,
      loading: false,
      routeLoading: false,
      locationTimer: null,
      routeTimer: null,
      statusTimer: null,
      lastRouteRefreshAt: 0
    }
  },
  computed: {
    statusText() {
      const map = {
        accepted: '待出餐',
        prepared: '待取餐',
        delivering: '配送中',
        delivered: '待确认',
        completed: '已完成',
        cancelled: '已取消'
      }
      return map[this.task.status] || '未知状态'
    },
    statusType() {
      const map = {
        accepted: 'info',
        prepared: 'primary',
        delivering: 'warning',
        delivered: 'success',
        completed: 'success',
        cancelled: 'error'
      }
      return map[this.task.status] || 'info'
    },
    heroTitle() {
      if (this.canDeliver) {
        return '系统已为你规划从当前位置到宿舍楼的推荐路线'
      }
      if (this.waitingPrepare) {
        return '订单已锁定，可提前查看从商户到宿舍楼的送餐路线'
      }
      if (this.canPickup) {
        return '商户已出餐，可按推荐路线完成本次配送'
      }
      return '查看订单详情与配送路线'
    },
    heroDescription() {
      if (this.routePlan) {
        return this.routeNoteText
      }
      return '路线会结合当前任务阶段自动切换，帮助你更快完成配送。'
    },
    canPickup() {
      return this.task.status === 'prepared'
    },
    canDeliver() {
      return this.task.status === 'delivering'
    },
    waitingPrepare() {
      return this.task.status === 'accepted'
    },
    merchantPoint() {
      return toPoint(this.task.merchantLatitude, this.task.merchantLongitude)
    },
    destinationPoint() {
      return toPoint(this.task.deliveryLatitude, this.task.deliveryLongitude)
    },
    riderPoint() {
      if (!this.canDeliver) {
        return null
      }
      return toPoint(
        this.currentPosition && this.currentPosition.latitude,
        this.currentPosition && this.currentPosition.longitude
      )
    },
    routePoints() {
      const points = Array.isArray(this.routePlan && this.routePlan.points) ? this.routePlan.points : []
      return points
        .map((point) => toPoint(point.latitude, point.longitude))
        .filter(Boolean)
    },
    hasMapData() {
      return this.routePoints.length > 1 || !!(this.merchantPoint && this.destinationPoint)
    },
    mapCenter() {
      if (this.routePoints.length > 1) {
        return midpoint(this.routePoints[0], this.routePoints[this.routePoints.length - 1])
      }
      if (this.riderPoint && this.destinationPoint) {
        return midpoint(this.riderPoint, this.destinationPoint)
      }
      return midpoint(this.merchantPoint, this.destinationPoint)
    },
    mapScale() {
      return this.canDeliver ? 15 : 16
    },
    mapIncludePoints() {
      return [this.merchantPoint, this.destinationPoint, this.riderPoint].filter(Boolean)
    },
    mapMarkers() {
      const markers = []
      if (this.merchantPoint) {
        markers.push(this.buildMarker(1, 'merchant', this.merchantPoint, this.task.merchantName || '商户'))
      }
      if (this.destinationPoint) {
        markers.push(this.buildMarker(2, 'destination', this.destinationPoint, '宿舍楼'))
      }
      if (this.riderPoint) {
        markers.push(this.buildMarker(3, 'rider', this.riderPoint, '当前位置'))
      }
      return markers
    },
    mapPolyline() {
      if (this.routePoints.length > 1) {
        return [
          {
            points: this.routePoints,
            color: '#1f7ae0',
            width: 8,
            arrowLine: this.canDeliver
          }
        ]
      }
      if (this.merchantPoint && this.destinationPoint) {
        return [
          {
            points: [this.merchantPoint, this.destinationPoint],
            color: '#8db7f0',
            width: 6,
            dottedLine: true
          }
        ]
      }
      return []
    },
    legends() {
      const items = [
        { key: 'merchant', label: '商户', color: '#ff9f43' },
        { key: 'destination', label: '宿舍楼', color: '#4c87ff' }
      ]
      if (this.canDeliver) {
        items.unshift({ key: 'rider', label: '当前位置', color: '#19be6b' })
      }
      return items
    },
    routeTip() {
      if (!this.routePlan) {
        return '系统正在准备路线'
      }
      if (this.routePlan.smart) {
        return this.canDeliver ? '已按当前位置重算最佳配送路线' : '已生成从商户到宿舍楼的推荐送餐路线'
      }
      return this.routePlan.provider === 'fallback' ? '当前为兜底预估路线' : '当前缺少可用坐标'
    },
    routeSteps() {
      const steps = Array.isArray(this.routePlan && this.routePlan.steps) ? this.routePlan.steps : []
      return steps.slice(0, 4)
    },
    distanceText() {
      if (!this.routePlan || this.routePlan.distanceMeters == null) {
        return '--'
      }
      const distance = Number(this.routePlan.distanceMeters)
      if (distance >= 1000) {
        return `${(distance / 1000).toFixed(1)}km`
      }
      return `${distance}m`
    },
    routeModeText() {
      const mode = (this.routePlan && this.routePlan.mode) || ''
      if (mode === 'EBICYCLING') return '电动车'
      if (mode === 'BICYCLING') return '自行车'
      if (mode === 'WALKING') return '步行'
      if (mode === 'DRIVING') return '驾车'
      return '直线预估'
    },
    displaySourceLabel() {
      if (!this.routePlan) {
        return '起点'
      }
      if (this.routePlan.sourceType === 'current') {
        return '当前位置'
      }
      return this.task.merchantName || '商户'
    },
    routeNoteText() {
      if (!this.routePlan) {
        return '系统正在生成推荐路线'
      }
      if (this.routePlan.smart) {
        return this.canDeliver
          ? '已启用腾讯路线规划，系统会按当前位置周期刷新推荐路线。'
          : '已启用腾讯路线规划，接单后即可提前熟悉从商户到宿舍楼的送餐路径。'
      }
      if (this.routePlan.provider === 'fallback') {
        return '当前未配置腾讯地图 Key 或算路失败，已自动切换为直线预估路线。'
      }
      return '商户或收货地址缺少坐标，暂时无法规划路线。'
    },
    routeStartHint() {
      if (!this.routePlan) {
        return '起点'
      }
      return this.routePlan.sourceType === 'current' ? '实时定位起点' : '取餐起点'
    }
  },
  onLoad(options) {
    this.taskId = options.id || ''
    this.loadTask({ forceRoute: true })
  },
  onShow() {
    if (this.taskId) {
      this.loadTask()
    }
  },
  onUnload() {
    this.stopLocationReport()
    this.stopRouteRefresh()
    this.stopStatusPolling()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    formatStepMeta(step) {
      const parts = []
      if (step.roadName) {
        parts.push(step.roadName)
      }
      if (step.distanceMeters != null) {
        parts.push(step.distanceMeters >= 1000 ? `${(step.distanceMeters / 1000).toFixed(1)}km` : `${step.distanceMeters}m`)
      }
      return parts.join(' · ')
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
    normalizeRoute(route) {
      if (!route) {
        return null
      }
      return {
        ...route,
        smart: !!route.smart,
        points: Array.isArray(route.points) ? route.points : [],
        steps: Array.isArray(route.steps) ? route.steps : []
      }
    },
    async loadTask(options = {}) {
      try {
        const previousStatus = this.task.status
        this.task = await getTaskDetail(this.taskId)
        this.$store.commit('SET_CURRENT_TASK', this.task)
        await this.syncRuntime(previousStatus, !!options.forceRoute)
      } catch (error) {
        console.error(error)
      }
    },
    async syncRuntime(previousStatus, forceRoute) {
      if (this.canDeliver) {
        this.startLocationReport()
        this.startRouteRefresh()
        this.stopStatusPolling()
        if (forceRoute || previousStatus !== 'delivering' || !this.routePlan || this.routePlan.sourceType !== 'current') {
          await this.refreshCurrentRoute(true)
        }
        return
      }

      this.stopLocationReport()
      this.stopRouteRefresh()

      if (this.waitingPrepare) {
        this.startStatusPolling()
      } else {
        this.stopStatusPolling()
      }

      if (['accepted', 'prepared'].includes(this.task.status)) {
        if (forceRoute || previousStatus !== this.task.status || !this.routePlan || this.routePlan.sourceType !== 'merchant') {
          await this.fetchRoutePlan()
        }
        return
      }

      this.routePlan = null
    },
    async fetchRoutePlan(params = {}) {
      this.routeLoading = true
      try {
        const route = await getTaskRoute(this.taskId, params)
        this.routePlan = this.normalizeRoute(route)
      } catch (error) {
        console.error(error)
      } finally {
        this.routeLoading = false
      }
    },
    captureLocation() {
      return new Promise((resolve) => {
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            resolve({
              latitude: res.latitude,
              longitude: res.longitude
            })
          },
          fail: () => resolve(null)
        })
      })
    },
    async reportLocation() {
      const point = await this.captureLocation()
      if (!point) {
        return null
      }
      this.currentPosition = point
      try {
        await updateLocation({
          latitude: point.latitude,
          longitude: point.longitude,
          orderId: this.taskId
        })
      } catch (error) {
        console.error(error)
      }
      return point
    },
    async refreshCurrentRoute(force = false) {
      if (!force && Date.now() - this.lastRouteRefreshAt < 55000) {
        return
      }
      let point = this.currentPosition
      if (!point) {
        point = await this.reportLocation()
      }
      await this.fetchRoutePlan(
        point
          ? {
              fromLat: point.latitude,
              fromLng: point.longitude
            }
          : {}
      )
      this.lastRouteRefreshAt = Date.now()
    },
    async handleRefreshRoute() {
      if (this.routeLoading) {
        return
      }
      if (this.canDeliver) {
        await this.refreshCurrentRoute(true)
        return
      }
      await this.fetchRoutePlan()
    },
    async handleAction() {
      if (this.canPickup) {
        await this.onPickup()
        return
      }
      if (this.canDeliver) {
        await this.onDeliver()
      }
    },
    async onPickup() {
      this.loading = true
      try {
        await pickupTask(this.taskId)
        uni.showToast({ title: '已确认取餐', icon: 'none' })
        await this.loadTask({ forceRoute: true })
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    async onDeliver() {
      this.loading = true
      try {
        await deliverTask(this.taskId)
        uni.showToast({ title: '已确认送达', icon: 'none' })
        this.stopLocationReport()
        this.stopRouteRefresh()
        this.$store.commit('SET_CURRENT_TASK', null)
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    openLocation(lat, lng, name) {
      if (lat === null || lat === undefined || lng === null || lng === undefined) {
        uni.showToast({ title: '暂无位置信息', icon: 'none' })
        return
      }
      uni.openLocation({
        latitude: Number(lat),
        longitude: Number(lng),
        name: name || '位置'
      })
    },
    callPhone(phone) {
      if (!phone) {
        uni.showToast({ title: '暂无联系电话', icon: 'none' })
        return
      }
      uni.makePhoneCall({ phoneNumber: phone })
    },
    startLocationReport() {
      if (this.locationTimer) {
        return
      }
      this.reportLocation()
      this.locationTimer = setInterval(() => {
        this.reportLocation()
      }, 15000)
    },
    stopLocationReport() {
      if (this.locationTimer) {
        clearInterval(this.locationTimer)
        this.locationTimer = null
      }
    },
    startRouteRefresh() {
      if (this.routeTimer) {
        return
      }
      this.routeTimer = setInterval(() => {
        this.refreshCurrentRoute()
      }, 60000)
    },
    stopRouteRefresh() {
      if (this.routeTimer) {
        clearInterval(this.routeTimer)
        this.routeTimer = null
      }
    },
    startStatusPolling() {
      if (this.statusTimer) {
        return
      }
      this.statusTimer = setInterval(() => {
        this.loadTask()
      }, 10000)
    },
    stopStatusPolling() {
      if (this.statusTimer) {
        clearInterval(this.statusTimer)
        this.statusTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 20rpx 140rpx;
  background:
    linear-gradient(180deg, #eef5ff 0rpx, #eef5ff 240rpx, #f5f7fa 240rpx, #f5f7fa 100%);
}

.hero-card,
.card {
  border-radius: 24rpx;
  background: #fff;
}

.hero-card {
  margin-bottom: 20rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #e8f2ff 0%, #ffffff 72%);
  box-shadow: 0 18rpx 40rpx rgba(31, 122, 224, 0.1);
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  box-shadow: 0 12rpx 28rpx rgba(25, 34, 53, 0.05);
}

.hero-head,
.section-head,
.stats-grid,
.info-row,
.steps-head,
.step-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.hero-head {
  margin-bottom: 18rpx;
}

.fee {
  color: #ff7a00;
  font-size: 34rpx;
  font-weight: 700;
}

.hero-title {
  display: block;
  color: #1f2430;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.4;
}

.hero-desc {
  display: block;
  margin-top: 10rpx;
  color: #6f7f92;
  font-size: 24rpx;
  line-height: 1.7;
}

.section-title {
  display: block;
  color: #1f2430;
  font-size: 30rpx;
  font-weight: 700;
}

.section-title.small {
  font-size: 28rpx;
}

.section-tip,
.steps-tip {
  color: #8a96a3;
  font-size: 22rpx;
}

.refresh-link {
  color: #1f7ae0;
  font-size: 24rpx;
  font-weight: 600;
}

.stats-grid {
  margin-top: 20rpx;
}

.stat-card {
  flex: 1;
  min-width: 0;
  padding: 20rpx;
  border-radius: 20rpx;
  background: #f5f8fd;
}

.stat-label {
  display: block;
  color: #8693a2;
  font-size: 22rpx;
}

.stat-value {
  display: block;
  margin-top: 10rpx;
  color: #1f2430;
  font-size: 30rpx;
  font-weight: 700;
}

.route-line {
  margin-top: 22rpx;
  padding: 20rpx;
  border-radius: 20rpx;
  background: #f8fbff;
}

.route-node {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.route-node + .route-node {
  margin-top: 18rpx;
}

.route-dot {
  width: 18rpx;
  height: 18rpx;
  margin-top: 8rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.route-dot.start {
  background: #1f7ae0;
  box-shadow: 0 0 0 8rpx rgba(31, 122, 224, 0.12);
}

.route-dot.end {
  background: #ff7a00;
  box-shadow: 0 0 0 8rpx rgba(255, 122, 0, 0.12);
}

.route-copy {
  flex: 1;
  min-width: 0;
}

.route-name {
  display: block;
  color: #1f2430;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1.5;
}

.route-sub {
  display: block;
  margin-top: 6rpx;
  color: #8b97a3;
  font-size: 22rpx;
}

.route-note {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  font-size: 22rpx;
  line-height: 1.6;
}

.route-note.smart {
  background: #eef6ff;
  color: #1f7ae0;
}

.route-note.fallback {
  background: #fff3e7;
  color: #ff7a00;
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
  gap: 18rpx;
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
  color: #778390;
  font-size: 22rpx;
}

.steps-box {
  margin-top: 22rpx;
}

.step-item {
  align-items: flex-start;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #edf1f6;
}

.step-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.step-index {
  width: 42rpx;
  height: 42rpx;
  line-height: 42rpx;
  text-align: center;
  border-radius: 50%;
  background: #edf5ff;
  color: #1f7ae0;
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.step-main {
  flex: 1;
  min-width: 0;
}

.step-text {
  display: block;
  color: #1f2430;
  font-size: 26rpx;
  line-height: 1.6;
}

.step-meta {
  display: block;
  margin-top: 8rpx;
  color: #8a96a3;
  font-size: 22rpx;
}

.info-row {
  justify-content: flex-start;
  margin-top: 18rpx;
  color: #1f2430;
  font-size: 28rpx;
}

.info-row text {
  margin-left: 12rpx;
  line-height: 1.6;
}

.link {
  color: #1f7ae0;
}

.notice-card {
  background: #f6f9ff;
}

.notice-text {
  display: block;
  margin-top: 14rpx;
  color: #6f7f92;
  font-size: 24rpx;
  line-height: 1.7;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 -8rpx 24rpx rgba(31, 36, 48, 0.08);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}
</style>
