<template>
  <view class="page">
    <view class="hero-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="hero-copy">
        <text class="hero-name">{{ profile.name || '骑手' }}</text>
        <text class="hero-phone">{{ profile.phone || '未绑定手机号' }}</text>
        <view :class="['status-pill', statusClass]">{{ statusText }}</view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">接单状态</text>
        <u-switch
          v-model="statusSwitch"
          active-color="#1f7ae0"
          @change="onStatusChange"
        ></u-switch>
      </view>
      <text class="panel-desc">切换为在线后，可以在首页查看并抢配送订单。</text>
    </view>

    <view class="stats-grid">
      <view class="stat-card">
        <text class="stat-value">{{ stats.todayDeliveries || 0 }}</text>
        <text class="stat-label">今日配送</text>
      </view>
      <view class="stat-card">
        <text class="stat-value money">¥{{ formatMoney(stats.todayEarnings) }}</text>
        <text class="stat-label">今日收入</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ profile.totalOrders || 0 }}</text>
        <text class="stat-label">累计配送</text>
      </view>
      <view class="stat-card">
        <text class="stat-value money">¥{{ formatMoney(profile.totalIncome) }}</text>
        <text class="stat-label">累计收入</text>
      </view>
    </view>

    <view class="info-card">
      <view class="info-row">
        <text class="info-label">骑手姓名</text>
        <text class="info-value">{{ profile.name || '--' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">手机号</text>
        <text class="info-value">{{ profile.phone || '--' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">当前状态</text>
        <text class="info-value">{{ statusText }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">当前位置</text>
        <text class="info-value">{{ locationText }}</text>
      </view>
    </view>

    <button class="logout-btn" @click="handleLogout">退出登录</button>
  </view>
</template>

<script>
import { getStats } from '@/api/stats'
import { getRiderProfile, updateRiderStatus } from '@/api/profile'
import { ensureLogin } from '@/utils/session'

export default {
  data() {
    return {
      profile: {},
      stats: {},
      statusSwitch: false,
      statusLoading: false
    }
  },
  computed: {
    avatarText() {
      const name = (this.profile.name || '').trim()
      return name ? name.slice(-1) : '骑'
    },
    statusText() {
      const statusMap = {
        online: '在线接单',
        offline: '离线休息',
        delivering: '配送中'
      }
      return statusMap[this.profile.status] || '离线休息'
    },
    statusClass() {
      return this.profile.status === 'online' || this.profile.status === 'delivering' ? 'online' : 'offline'
    },
    locationText() {
      if (this.profile.latitude === undefined || this.profile.latitude === null || this.profile.longitude === undefined || this.profile.longitude === null) {
        return '暂未上传'
      }
      return `${this.profile.latitude}, ${this.profile.longitude}`
    }
  },
  onShow() {
    if (!ensureLogin()) {
      return
    }
    this.loadData()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    syncStoreProfile(profile) {
      this.$store.commit('SET_RIDER_STATUS', profile.status || 'offline')
      this.$store.commit('SET_USER_INFO', {
        ...(this.$store.state.userInfo || {}),
        nickname: profile.name || '',
        phone: profile.phone || '',
        avatar: profile.avatar || '',
        role: 'rider'
      })
    },
    async loadData() {
      try {
        const [profile, stats] = await Promise.all([getRiderProfile(), getStats()])
        this.profile = profile || {}
        this.stats = stats || {}
        this.statusSwitch = !!(profile && profile.status && profile.status !== 'offline')
        this.syncStoreProfile(this.profile)
      } catch (error) {
        console.error(error)
      }
    },
    async onStatusChange(value) {
      if (this.statusLoading) {
        return
      }
      this.statusLoading = true
      const nextStatus = value ? 'online' : 'offline'
      try {
        await updateRiderStatus(nextStatus)
        this.profile = {
          ...this.profile,
          status: nextStatus
        }
        this.statusSwitch = value
        this.syncStoreProfile(this.profile)
        uni.showToast({
          title: value ? '已开始接单' : '已切换离线',
          icon: 'none'
        })
      } catch (error) {
        this.statusSwitch = !!(this.profile.status && this.profile.status !== 'offline')
      } finally {
        this.statusLoading = false
      }
    },
    handleLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定退出当前骑手账号吗？',
        success: ({ confirm }) => {
          if (!confirm) {
            return
          }
          this.$store.dispatch('logout')
          uni.showToast({ title: '已退出登录', icon: 'none' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/index' })
          }, 500)
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background:
    linear-gradient(180deg, #eef5ff 0rpx, #eef5ff 220rpx, #f5f7fa 220rpx, #f5f7fa 100%);
}

.hero-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 32rpx 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #1f7ae0 0%, #4aa6ff 100%);
  color: #fff;
  box-shadow: 0 16rpx 34rpx rgba(31, 122, 224, 0.18);
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: 700;
}

.hero-copy {
  flex: 1;
}

.hero-name {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
}

.hero-phone {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 18rpx;
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.status-pill.online {
  background: rgba(255, 255, 255, 0.18);
}

.status-pill.offline {
  background: rgba(19, 33, 57, 0.18);
}

.panel,
.info-card {
  margin-top: 22rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 28rpx rgba(22, 34, 51, 0.06);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  color: #1f2430;
  font-size: 30rpx;
  font-weight: 700;
}

.panel-desc {
  display: block;
  margin-top: 12rpx;
  color: #8090a0;
  font-size: 24rpx;
  line-height: 1.6;
}

.stats-grid {
  margin-top: 22rpx;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.stat-card {
  width: calc(50% - 9rpx);
  padding: 26rpx 22rpx;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 12rpx 28rpx rgba(22, 34, 51, 0.05);
  margin-bottom: 18rpx;
}

.stat-value {
  display: block;
  color: #1f2430;
  font-size: 38rpx;
  font-weight: 700;
}

.stat-value.money {
  color: #ff7a00;
}

.stat-label {
  display: block;
  margin-top: 8rpx;
  color: #8793a2;
  font-size: 24rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.info-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-label {
  color: #7e8a98;
  font-size: 24rpx;
}

.info-value {
  max-width: 60%;
  text-align: right;
  color: #1f2430;
  font-size: 26rpx;
}

.logout-btn {
  margin-top: 28rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 24rpx;
  background: #fff;
  color: #e05b5b;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
