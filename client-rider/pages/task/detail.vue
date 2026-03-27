<template>
  <view class="container">
    <view class="task-card" v-if="task.id">
      <view class="card-header">
        <u-tag :text="statusText" :type="statusType" size="mini"></u-tag>
        <text class="fee">配送费 ¥{{ formatMoney(task.deliveryFee) }}</text>
      </view>

      <view class="info-section">
        <view class="section-label">商家信息</view>
        <view class="info-row">
          <u-icon name="shop" size="28"></u-icon>
          <text>{{ task.merchantName }}</text>
        </view>
        <view class="info-row" @click="openLocation(task.merchantLatitude, task.merchantLongitude, task.merchantAddress)">
          <u-icon name="map" size="28" color="#1890ff"></u-icon>
          <text class="link">{{ task.merchantAddress }}</text>
        </view>
      </view>

      <view class="info-section">
        <view class="section-label">配送信息</view>
        <view class="info-row">
          <u-icon name="account" size="28"></u-icon>
          <text>{{ task.customerName || '收货人' }}</text>
        </view>
        <view class="info-row" @click="callPhone(task.customerPhone)">
          <u-icon name="phone" size="28" color="#1890ff"></u-icon>
          <text class="link">{{ task.customerPhone }}</text>
        </view>
        <view class="info-row" @click="openLocation(task.deliveryLatitude, task.deliveryLongitude, task.deliveryAddress)">
          <u-icon name="map-fill" size="28" color="#1890ff"></u-icon>
          <text class="link">{{ task.deliveryAddress }}</text>
        </view>
      </view>

      <view class="info-section">
        <view class="section-label">订单信息</view>
        <view class="info-row">
          <u-icon name="file-text" size="28"></u-icon>
          <text>订单号：{{ task.orderNo }}</text>
        </view>
        <view class="info-row" v-if="task.estimatedTime">
          <u-icon name="clock" size="28"></u-icon>
          <text>预计送达：{{ task.estimatedTime }}</text>
        </view>
      </view>

      <view class="info-section notice-section" v-if="waitingPrepare">
        <view class="section-label">当前进度</view>
        <view class="remark">你已抢到这单，商户出餐后页面会自动刷新为“待取餐”。</view>
      </view>

      <view class="info-section" v-if="task.remark">
        <view class="section-label">备注</view>
        <view class="remark">{{ task.remark }}</view>
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
import { deliverTask, getTaskDetail, pickupTask } from '@/api/task'
import { updateLocation } from '@/api/location'

export default {
  data() {
    return {
      taskId: '',
      task: {},
      loading: false,
      locationTimer: null,
      statusTimer: null
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
    canPickup() {
      return this.task.status === 'prepared'
    },
    canDeliver() {
      return this.task.status === 'delivering'
    },
    waitingPrepare() {
      return this.task.status === 'accepted'
    }
  },
  onLoad(options) {
    this.taskId = options.id || ''
    this.loadTask()
  },
  onShow() {
    if (this.taskId) {
      this.loadTask()
    }
  },
  onUnload() {
    this.stopLocationReport()
    this.stopStatusPolling()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    async loadTask() {
      try {
        this.task = await getTaskDetail(this.taskId)
        this.$store.commit('SET_CURRENT_TASK', this.task)
        if (this.canDeliver) {
          this.startLocationReport()
        } else {
          this.stopLocationReport()
        }
        if (this.waitingPrepare) {
          this.startStatusPolling()
        } else {
          this.stopStatusPolling()
        }
      } catch (e) {
        console.error(e)
      }
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
        await this.loadTask()
      } catch (e) {
        console.error(e)
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
        this.$store.commit('SET_CURRENT_TASK', null)
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    reportLocation() {
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          updateLocation({
            latitude: res.latitude,
            longitude: res.longitude,
            orderId: this.taskId
          })
        }
      })
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
.container {
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.task-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #eee;
}

.fee {
  font-size: 36rpx;
  font-weight: bold;
  color: #ff6b00;
}

.info-section {
  margin-bottom: 24rpx;
}

.section-label {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.notice-section {
  background: #f5f9ff;
  border-radius: 16rpx;
  padding: 20rpx;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
  font-size: 28rpx;
}

.info-row text {
  margin-left: 12rpx;
}

.link {
  color: #1890ff;
}

.remark {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -8rpx 24rpx rgba(31, 36, 48, 0.08);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}
</style>
