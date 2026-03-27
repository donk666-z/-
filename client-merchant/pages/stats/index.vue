<template>
  <view class="container">
    <view class="header">
      <text class="title">数据统计</text>
      <text class="date">{{ today }}</text>
    </view>

    <view class="stats-grid">
      <view class="stat-card">
        <text class="stat-value">{{ dashboard.todayOrders || 0 }}</text>
        <text class="stat-label">今日订单数</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">¥{{ dashboard.todayRevenue || '0.00' }}</text>
        <text class="stat-label">今日营收</text>
      </view>
    </view>

    <view class="stats-grid">
      <view class="stat-card warning">
        <text class="stat-value">{{ dashboard.pendingCount || 0 }}</text>
        <text class="stat-label">待处理订单</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ dashboard.totalDishes || 0 }}</text>
        <text class="stat-label">在售菜品</text>
      </view>
    </view>

    <view class="section" v-if="dashboard.recentOrders && dashboard.recentOrders.length">
      <text class="section-title">近期订单</text>
      <view v-for="item in dashboard.recentOrders" :key="item.id" class="recent-item">
        <text class="recent-no">{{ item.orderNo }}</text>
        <text class="recent-price">¥{{ item.totalPrice }}</text>
        <text class="recent-time">{{ item.createTime }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getDashboard } from '@/api/stats'

export default {
  data() {
    return {
      dashboard: {},
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
        this.dashboard = await getDashboard()
      } catch (e) {
        console.error('加载统计数据失败', e)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 30rpx;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40rpx;
  .title { font-size: 44rpx; font-weight: bold; color: #333; }
  .date { font-size: 28rpx; color: #999; }
}
.stats-grid {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  .stat-value { display: block; font-size: 44rpx; font-weight: bold; color: #333; margin-bottom: 10rpx; }
  .stat-label { font-size: 24rpx; color: #999; }
  &.warning .stat-value { color: #ff6b00; }
}
.section {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-top: 20rpx;
  .section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 20rpx; display: block; }
}
.recent-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  .recent-no { font-size: 26rpx; color: #333; flex: 1; }
  .recent-price { font-size: 26rpx; color: #ff6b00; width: 150rpx; text-align: right; }
  .recent-time { font-size: 24rpx; color: #999; width: 200rpx; text-align: right; }
}
</style>
