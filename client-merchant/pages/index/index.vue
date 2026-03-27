<template>
  <view class="container">
    <view class="header">
      <view class="header-left">
        <text class="title">工作台</text>
        <text class="date">{{ today }}</text>
      </view>
      <view class="logout-btn" @click="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <view class="stats-grid">
      <view class="stat-card">
        <text class="stat-value">{{ dashboard.todayOrders || 0 }}</text>
        <text class="stat-label">今日订单</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">¥{{ dashboard.todayRevenue || '0.00' }}</text>
        <text class="stat-label">今日营收</text>
      </view>
      <view class="stat-card warning">
        <text class="stat-value">{{ dashboard.pendingCount || 0 }}</text>
        <text class="stat-label">待处理订单</text>
      </view>
    </view>

    <view class="quick-actions">
      <text class="section-title">快捷操作</text>
      <view class="action-grid">
        <view class="action-item" @click="goTo('/pages/order/list')">
          <u-icon name="list" size="48" color="#FF6B00"></u-icon>
          <text>订单管理</text>
        </view>
        <view class="action-item" @click="goTo('/pages/dish/list')">
          <u-icon name="edit-pen" size="48" color="#FF6B00"></u-icon>
          <text>菜品管理</text>
        </view>
        <view class="action-item" @click="goTo('/pages/stats/index')">
          <u-icon name="bar-chart" size="48" color="#FF6B00"></u-icon>
          <text>数据统计</text>
        </view>
        <view class="action-item" @click="goTo('/pages/shop/settings')">
          <u-icon name="setting" size="48" color="#FF6B00"></u-icon>
          <text>店铺设置</text>
        </view>
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
    this.loadDashboard()
    const d = new Date()
    this.today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  },
  methods: {
    async loadDashboard() {
      try {
        this.dashboard = await getDashboard()
        this.$store.commit('SET_PENDING_COUNT', this.dashboard.pendingCount || 0)
      } catch (e) {
        console.error('加载工作台数据失败', e)
      }
    },
    goTo(url) {
      if (['/pages/order/list', '/pages/dish/list', '/pages/stats/index', '/pages/shop/settings'].includes(url)) {
        uni.switchTab({ url })
      } else {
        uni.navigateTo({ url })
      }
    },
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.$store.commit('LOGOUT')
            uni.navigateTo({ url: '/pages/login/index' })
          }
        }
      })
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
  .header-left {
    display: flex;
    flex-direction: column;
  }
  .title { font-size: 44rpx; font-weight: bold; color: #333; }
  .date { font-size: 28rpx; color: #999; margin-top: 10rpx; }
  .logout-btn {
    padding: 15rpx 30rpx;
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 30rpx;
    .logout-text { font-size: 26rpx; color: #666; }
  }
}
.stats-grid {
  display: flex;
  gap: 20rpx;
  margin-bottom: 40rpx;
}
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  .stat-value { display: block; font-size: 40rpx; font-weight: bold; color: #333; margin-bottom: 10rpx; }
  .stat-label { font-size: 24rpx; color: #999; }
  &.warning .stat-value { color: #ff6b00; }
}
.quick-actions {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  .section-title { font-size: 32rpx; font-weight: bold; color: #333; margin-bottom: 30rpx; }
}
.action-grid {
  display: flex;
  flex-wrap: wrap;
}
.action-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  text { font-size: 24rpx; color: #666; margin-top: 12rpx; }
}
</style>
