<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-main">
        <text class="hero-title">数据统计</text>
        <text class="hero-subtitle">看看今天表现、近 7 日走势和热销菜品</text>
      </view>
      <view class="date-chip">{{ today }}</view>
    </view>

    <view class="overview-grid">
      <view class="overview-card">
        <text class="card-label">今日订单</text>
        <text class="card-value">{{ dashboard.todayOrders || 0 }}</text>
        <text class="card-note">已完成订单</text>
      </view>

      <view class="overview-card accent">
        <text class="card-label">今日营收</text>
        <text class="card-value">¥{{ dashboard.todayRevenueText }}</text>
        <text class="card-note">已确认收货后入账</text>
      </view>

      <view class="overview-card warning">
        <text class="card-label">待处理订单</text>
        <text class="card-value">{{ dashboard.pendingCount || 0 }}</text>
        <text class="card-note">等待商户处理</text>
      </view>

      <view class="overview-card">
        <text class="card-label">在售菜品</text>
        <text class="card-value">{{ dashboard.totalDishes || 0 }}</text>
        <text class="card-note">当前上架菜品数</text>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <view>
          <text class="section-title">经营概览</text>
          <text class="section-subtitle">快速判断近一周经营节奏</text>
        </view>
      </view>

      <view class="insight-grid">
        <view class="insight-card accent">
          <text class="insight-label">周销售额</text>
          <text class="insight-value">¥{{ dashboard.weeklyRevenueText }}</text>
          <text class="insight-note">近 7 天确认收货后入账</text>
        </view>

        <view class="insight-card">
          <text class="insight-label">周完成订单</text>
          <text class="insight-value">{{ dashboard.weeklyOrders || 0 }}</text>
          <text class="insight-note">近 7 天累计完成单量</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <view>
          <text class="section-title">近 7 日销售</text>
          <text class="section-subtitle">对比每天营收和完成单量</text>
        </view>
      </view>

      <view v-if="dashboard.weeklyTrend.length" class="trend-list">
        <view v-for="item in dashboard.weeklyTrend" :key="item.date" class="trend-item">
          <view class="trend-meta">
            <text class="trend-label">{{ item.label }}</text>
            <text class="trend-orders">{{ item.orderCount }} 单</text>
          </view>
          <view class="trend-main">
            <view class="trend-track">
              <view class="trend-bar" :style="{ width: item.barWidth }"></view>
            </view>
            <text class="trend-value">¥{{ item.revenueText }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-wrap compact">
        <u-empty text="近 7 天暂无销售记录" mode="list"></u-empty>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <view>
          <text class="section-title">热门菜品</text>
          <text class="section-subtitle">近 7 天销量靠前的菜品</text>
        </view>
      </view>

      <view v-if="dashboard.hotDishes.length" class="rank-list">
        <view v-for="item in dashboard.hotDishes" :key="item.rank" class="rank-item">
          <view class="rank-badge" :class="'rank-' + item.rank">{{ item.rank }}</view>

          <image v-if="item.image" :src="item.image" class="rank-image" mode="aspectFill"></image>
          <view v-else class="rank-image rank-image-placeholder">
            <u-icon name="photo" size="30" color="#c1c7d0"></u-icon>
          </view>

          <view class="rank-main">
            <view class="rank-top">
              <text class="rank-name">{{ item.name }}</text>
              <text class="rank-type">{{ item.typeText }}</text>
            </view>
            <text class="rank-meta">近 7 天售出 {{ item.salesCount }} 份</text>
          </view>

          <view class="rank-side">
            <text class="rank-revenue">¥{{ item.revenueText }}</text>
            <text class="rank-note">销售额</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-wrap compact">
        <u-empty text="近 7 天暂无热销菜品数据" mode="list"></u-empty>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <view>
          <text class="section-title">最近订单</text>
          <text class="section-subtitle">最近 5 笔下单记录</text>
        </view>
      </view>

      <view v-if="dashboard.recentOrders.length" class="recent-list">
        <view v-for="item in dashboard.recentOrders" :key="item.id" class="recent-item">
          <view class="recent-main">
            <text class="recent-no">{{ item.orderNo }}</text>
            <text class="recent-time">{{ item.createTime }}</text>
          </view>
          <view class="recent-side">
            <text class="recent-price">¥{{ item.totalPriceText }}</text>
            <text class="recent-status" :class="item.statusClassName">{{ item.statusText }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-wrap">
        <u-empty text="暂时还没有订单记录" mode="list"></u-empty>
      </view>
    </view>
  </view>
</template>

<script>
import { getDashboard } from '@/api/stats'

const createDefaultDashboard = () => ({
  todayOrders: 0,
  todayRevenue: 0,
  todayRevenueText: '0.00',
  pendingCount: 0,
  totalDishes: 0,
  weeklyRevenue: 0,
  weeklyRevenueText: '0.00',
  weeklyOrders: 0,
  weeklyTrend: [],
  hotDishes: [],
  recentOrders: []
})

export default {
  data() {
    return {
      dashboard: createDefaultDashboard(),
      today: ''
    }
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.loadData()
    const d = new Date()
    this.today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  },
  methods: {
    async loadData() {
      try {
        const data = await getDashboard()
        this.dashboard = this.normalizeDashboard(data)
      } catch (e) {
        this.dashboard = createDefaultDashboard()
        console.error('加载统计数据失败', e)
      }
    },
    normalizeDashboard(data = {}) {
      const pendingSource = data.pendingCount !== undefined && data.pendingCount !== null
        ? data.pendingCount
        : data.pendingOrders
      const pendingCount = Number(pendingSource || 0)
      const weeklyTrendRaw = Array.isArray(data.weeklyTrend) ? data.weeklyTrend : []
      const maxTrendRevenue = weeklyTrendRaw.reduce((max, item) => {
        const revenue = Number(item && item.revenue ? item.revenue : 0)
        return revenue > max ? revenue : max
      }, 0)
      const weeklyTrend = weeklyTrendRaw.map((item) => {
        const revenue = Number(item && item.revenue ? item.revenue : 0)
        const ratio = maxTrendRevenue > 0 ? (revenue / maxTrendRevenue) * 100 : 0
        return {
          date: item.date || item.label || '',
          label: item.label || '',
          orderCount: Number(item.orderCount || 0),
          revenue,
          revenueText: this.formatMoney(revenue),
          barWidth: ratio > 0 ? `${Math.max(ratio, 14)}%` : '0%'
        }
      })
      const hotDishes = Array.isArray(data.hotDishes)
        ? data.hotDishes.map((item) => ({
          ...item,
          salesCount: Number(item.salesCount || 0),
          revenueText: this.formatMoney(item.revenue),
          typeText: this.formatDishType(item.type)
        }))
        : []
      const recentOrders = Array.isArray(data.recentOrders)
        ? data.recentOrders.map((item) => ({
          ...item,
          totalPriceText: this.formatMoney(item.totalPrice),
          statusText: this.formatStatus(item.status),
          statusClassName: this.statusClass(item.status)
        }))
        : []

      return {
        todayOrders: Number(data.todayOrders || 0),
        todayRevenue: Number(data.todayRevenue || 0),
        todayRevenueText: this.formatMoney(data.todayRevenue),
        pendingCount,
        totalDishes: Number(data.totalDishes || 0),
        weeklyRevenue: Number(data.weeklyRevenue || 0),
        weeklyRevenueText: this.formatMoney(data.weeklyRevenue),
        weeklyOrders: Number(data.weeklyOrders || 0),
        weeklyTrend,
        hotDishes,
        recentOrders
      }
    },
    formatMoney(value) {
      const amount = Number(value || 0)
      return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
    },
    formatDishType(type) {
      return type === 'combo' ? '套餐' : '单品'
    },
    formatStatus(status) {
      const map = {
        paid: '待接单',
        accepted: '已接单',
        prepared: '待取餐',
        delivering: '配送中',
        delivered: '待收货',
        completed: '已完成',
        cancelled: '已取消'
      }
      return map[status] || '处理中'
    },
    statusClass(status) {
      const map = {
        paid: 'pending',
        accepted: 'pending',
        prepared: 'pending',
        delivering: 'warning',
        delivered: 'warning',
        completed: 'success',
        cancelled: 'muted'
      }
      return map[status] || 'pending'
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #fff8f2 0%, #f6f7fb 240rpx, #f6f7fb 100%);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 14rpx 36rpx rgba(20, 34, 61, 0.06);
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #8b93a6;
}

.date-chip {
  flex-shrink: 0;
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: #fff3e7;
  color: #ff7a1a;
  font-size: 24rpx;
  font-weight: 600;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 24rpx;
}

.overview-card {
  min-height: 174rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(18, 38, 63, 0.06);
}

.overview-card.accent {
  background: linear-gradient(135deg, #fff4e8 0%, #ffffff 100%);
}

.overview-card.warning .card-value {
  color: #ff7a1a;
}

.card-label {
  display: block;
  font-size: 24rpx;
  color: #8b93a6;
}

.card-value {
  display: block;
  margin-top: 18rpx;
  font-size: 48rpx;
  font-weight: 700;
  color: #1f2937;
}

.card-note {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #a1a9b8;
}

.section-card {
  margin-top: 24rpx;
  padding: 26rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 14rpx 36rpx rgba(20, 34, 61, 0.06);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.section-subtitle {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 8rpx;
}

.insight-card {
  padding: 24rpx 22rpx;
  border-radius: 22rpx;
  background: #f9fafc;
}

.insight-card.accent {
  background: linear-gradient(135deg, #fff4e7 0%, #fffaf6 100%);
}

.insight-label {
  display: block;
  font-size: 24rpx;
  color: #8b93a6;
}

.insight-value {
  display: block;
  margin-top: 16rpx;
  font-size: 42rpx;
  font-weight: 700;
  color: #1f2937;
}

.insight-note {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}

.trend-list {
  margin-top: 8rpx;
}

.trend-item {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f2f4f7;
}

.trend-item:last-child {
  border-bottom: none;
}

.trend-meta,
.trend-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.trend-main {
  margin-top: 10rpx;
}

.trend-label {
  font-size: 24rpx;
  color: #5d6778;
}

.trend-orders {
  font-size: 22rpx;
  color: #9aa3b2;
}

.trend-track {
  flex: 1;
  height: 12rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #f1f4f8;
}

.trend-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #ffb26b 0%, #ff7a1a 100%);
}

.trend-value {
  flex-shrink: 0;
  min-width: 120rpx;
  text-align: right;
  font-size: 24rpx;
  font-weight: 600;
  color: #1f2937;
}

.rank-list {
  margin-top: 8rpx;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f2f4f7;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 16rpx;
  background: #f1f4f8;
  font-size: 24rpx;
  font-weight: 700;
  color: #7b8495;
}

.rank-badge.rank-1 {
  background: #fff1e4;
  color: #ff7a1a;
}

.rank-badge.rank-2 {
  background: #f3f5f8;
  color: #667085;
}

.rank-badge.rank-3 {
  background: #fff5ef;
  color: #d9822b;
}

.rank-image {
  width: 96rpx;
  height: 96rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
  background: #f4f6fa;
}

.rank-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-top {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.rank-name {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 700;
  color: #243041;
}

.rank-type {
  flex-shrink: 0;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #fff3e7;
  font-size: 20rpx;
  color: #ff7a1a;
}

.rank-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #8f98a8;
}

.rank-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
  flex-shrink: 0;
}

.rank-revenue {
  font-size: 28rpx;
  font-weight: 700;
  color: #ff6b00;
}

.rank-note {
  font-size: 20rpx;
  color: #9aa3b2;
}

.recent-list {
  margin-top: 8rpx;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f2f4f7;
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-main {
  flex: 1;
  min-width: 0;
}

.recent-no {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #243041;
}

.recent-time {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}

.recent-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
}

.recent-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ff6b00;
}

.recent-status {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.recent-status.pending {
  background: #fff3e7;
  color: #ff7a1a;
}

.recent-status.warning {
  background: rgba(255, 170, 0, 0.12);
  color: #d9822b;
}

.recent-status.success {
  background: rgba(25, 190, 107, 0.12);
  color: #19be6b;
}

.recent-status.muted {
  background: #f1f4f8;
  color: #8e97a8;
}

.empty-wrap {
  padding: 40rpx 0 12rpx;
}

.empty-wrap.compact {
  padding-top: 12rpx;
}
</style>
