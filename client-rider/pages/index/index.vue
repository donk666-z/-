<template>
  <view class="container">
    <view class="hero-card" @click="goProfile">
      <view class="hero-copy">
        <text class="hero-title">{{ riderName }}</text>
        <text class="hero-desc">查看个人资料、接单状态和退出登录</text>
      </view>
      <view class="hero-action">
        <text class="hero-status">{{ statusText }}</text>
        <u-icon name="arrow-right" size="18" color="#5f6b7a"></u-icon>
      </view>
    </view>

    <view class="status-bar">
      <text class="status-label">接单状态</text>
      <u-switch v-model="isOnline" :loading="statusLoading" active-color="#1890ff" @change="onStatusChange"></u-switch>
      <text class="status-text">{{ statusText }}</text>
    </view>

    <view class="current-task" v-if="currentTask" @click="goToDetail(currentTask.id)">
      <view class="task-header">
        <u-tag text="配送中" type="warning" size="mini"></u-tag>
        <text class="task-fee">¥{{ formatMoney(currentTask.deliveryFee) }}</text>
      </view>
      <view class="task-merchant">{{ currentTask.merchantName }}</view>
      <view class="task-address">{{ currentTask.deliveryAddress }}</view>
    </view>

    <view class="section-title">可接订单</view>

    <view class="task-list" v-if="isOnline">
      <view class="task-item" v-for="item in taskList" :key="item.id">
        <view class="task-info">
          <view class="merchant-name">{{ item.merchantName }}</view>
          <view class="merchant-address">
            <u-icon name="map" size="24"></u-icon>
            <text>{{ item.merchantAddress }}</text>
          </view>
          <view class="delivery-address">
            <u-icon name="map-fill" size="24" color="#1890ff"></u-icon>
            <text>{{ item.deliveryAddress }}</text>
          </view>
          <view class="task-meta">
            <text class="fee">配送费 ¥{{ formatMoney(item.deliveryFee) }}</text>
            <text class="distance" v-if="item.distance">{{ item.distance }}km</text>
          </view>
        </view>
        <view class="task-action">
          <u-button type="primary" size="small" @click="onGrabTask(item.id)">抢单</u-button>
        </view>
      </view>
      <u-empty v-if="taskList.length === 0" text="暂无可接订单" mode="list"></u-empty>
    </view>

    <view class="offline-tip" v-else>
      <u-empty text="开启在线接单后才会展示待抢订单" mode="list"></u-empty>
    </view>
  </view>
</template>

<script>
import { getAvailableTasks, grabTask, getCurrentTask } from '@/api/task'
import { getRiderProfile, updateRiderStatus } from '@/api/profile'
import { ensureLogin } from '@/utils/session'

export default {
  data() {
    return {
      isOnline: false,
      taskList: [],
      currentTask: null,
      statusLoading: false
    }
  },
  computed: {
    riderName() {
      const userInfo = this.$store.state.userInfo || {}
      return userInfo.nickname || userInfo.phone || '骑手工作台'
    },
    statusText() {
      if (this.currentTask) {
        return '配送中'
      }
      return this.isOnline ? '在线接单' : '离线休息'
    }
  },
  onShow() {
    if (!ensureLogin()) {
      return
    }
    this.bootstrapPage()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    syncProfile(profile) {
      this.$store.commit('SET_RIDER_STATUS', profile.status || 'offline')
      this.$store.commit('SET_USER_INFO', {
        ...(this.$store.state.userInfo || {}),
        nickname: profile.name || '',
        phone: profile.phone || '',
        avatar: profile.avatar || ''
      })
      this.isOnline = profile.status === 'online' || profile.status === 'delivering'
    },
    async bootstrapPage() {
      try {
        const profile = await getRiderProfile()
        this.syncProfile(profile)
      } catch (error) {
        console.error(error)
      }

      await this.loadCurrentTask()
      if (this.isOnline) {
        await this.loadTasks()
      } else {
        this.taskList = []
      }
    },
    async onStatusChange(value) {
      if (!value && this.currentTask) {
        this.isOnline = true
        uni.showToast({ title: '当前有配送任务，暂不能下线', icon: 'none' })
        return
      }

      this.statusLoading = true
      try {
        const nextStatus = value ? 'online' : 'offline'
        await updateRiderStatus(nextStatus)
        this.$store.commit('SET_RIDER_STATUS', nextStatus)
        this.isOnline = value
        if (value) {
          await this.loadTasks()
          uni.showToast({ title: '已开始接单', icon: 'none' })
        } else {
          this.taskList = []
          uni.showToast({ title: '已切换离线', icon: 'none' })
        }
      } catch (error) {
        this.isOnline = !value
      } finally {
        this.statusLoading = false
      }
    },
    async loadTasks() {
      try {
        this.taskList = await getAvailableTasks()
      } catch (error) {
        console.error(error)
      }
    },
    async loadCurrentTask() {
      try {
        this.currentTask = await getCurrentTask()
        this.$store.commit('SET_CURRENT_TASK', this.currentTask)
      } catch (error) {
        this.currentTask = null
        this.$store.commit('SET_CURRENT_TASK', null)
      }
    },
    async onGrabTask(id) {
      try {
        await grabTask(id)
        uni.showToast({ title: '抢单成功', icon: 'none' })
        await this.loadTasks()
        await this.loadCurrentTask()
      } catch (error) {
        console.error(error)
      }
    },
    goToDetail(id) {
      uni.navigateTo({
        url: `/pages/task/detail?id=${id}`
      })
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

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  padding: 28rpx 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fbff 100%);
  border: 2rpx solid #dfeaf8;
}

.hero-copy {
  flex: 1;
}

.hero-title {
  display: block;
  color: #1f2430;
  font-size: 32rpx;
  font-weight: 700;
}

.hero-desc {
  display: block;
  margin-top: 8rpx;
  color: #7c8795;
  font-size: 24rpx;
}

.hero-action {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.hero-status {
  color: #1f7ae0;
  font-size: 24rpx;
  font-weight: 600;
}

.status-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.status-label {
  font-size: 28rpx;
  color: #333;
  margin-right: 20rpx;
}

.status-text {
  font-size: 26rpx;
  color: #999;
  margin-left: 16rpx;
}

.current-task {
  background: #fff3e0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border-left: 8rpx solid #ff9800;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.task-fee {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b00;
}

.task-merchant {
  font-size: 28rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}

.task-address {
  font-size: 24rpx;
  color: #666;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.task-item {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  align-items: center;
}

.task-info {
  flex: 1;
}

.merchant-name {
  font-size: 30rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}

.merchant-address,
.delivery-address {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.merchant-address text,
.delivery-address text {
  margin-left: 8rpx;
}

.task-meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}

.fee {
  font-size: 28rpx;
  color: #ff6b00;
  font-weight: bold;
  margin-right: 20rpx;
}

.distance {
  font-size: 24rpx;
  color: #999;
}

.task-action {
  margin-left: 20rpx;
}

.offline-tip {
  margin-top: 100rpx;
}
</style>
