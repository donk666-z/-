<template>
  <view class="container">
    <view class="page-head">
      <view>
        <text class="page-title">收入统计</text>
        <text class="page-subtitle">查看今日与累计配送收入表现</text>
      </view>
      <view class="profile-link" @click="goProfile">个人资料</view>
    </view>

    <view class="stats-card today">
      <view class="card-title">今日数据</view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ stats.todayDeliveries || 0 }}</text>
          <text class="stat-label">配送单数</text>
        </view>
        <view class="stat-item">
          <text class="stat-value earnings">¥{{ formatMoney(stats.todayEarnings) }}</text>
          <text class="stat-label">今日收入</text>
        </view>
      </view>
    </view>

    <view class="stats-card total">
      <view class="card-title">累计数据</view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ stats.totalDeliveries || 0 }}</text>
          <text class="stat-label">累计配送</text>
        </view>
        <view class="stat-item">
          <text class="stat-value earnings">¥{{ formatMoney(stats.totalEarnings) }}</text>
          <text class="stat-label">累计收入</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getStats } from '@/api/stats'
import { ensureLogin } from '@/utils/session'

export default {
  data() {
    return {
      stats: {}
    }
  },
  onShow() {
    if (!ensureLogin()) {
      return
    }
    this.loadStats()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    async loadStats() {
      try {
        this.stats = await getStats()
      } catch (error) {
        console.error(error)
      }
    },
    goProfile() {
      uni.navigateTo({ url: '/pages/profile/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.page-title {
  display: block;
  color: #1f2430;
  font-size: 34rpx;
  font-weight: 700;
}

.page-subtitle {
  display: block;
  margin-top: 8rpx;
  color: #8a94a3;
  font-size: 24rpx;
}

.profile-link {
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: #eaf3ff;
  color: #1f7ae0;
  font-size: 24rpx;
  font-weight: 600;
}

.stats-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 24rpx;
  color: #333;
}

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.stat-value.earnings {
  color: #ff6b00;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.stats-card.today {
  border-top: 6rpx solid #1890ff;
}

.stats-card.total {
  border-top: 6rpx solid #36cfc9;
}
</style>
