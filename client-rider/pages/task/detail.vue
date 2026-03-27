<template>
  <view class="container">
    <view class="task-card">
      <view class="card-header">
        <u-tag :text="statusText" :type="statusType" size="mini"></u-tag>
        <text class="fee">配送费 ¥{{ task.deliveryFee }}</text>
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
          <text>{{ task.customerName }}</text>
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

      <view class="info-section" v-if="task.remark">
        <view class="section-label">备注</view>
        <view class="remark">{{ task.remark }}</view>
      </view>
    </view>

    <view class="action-bar" v-if="task.status === 'delivering'">
      <u-button type="primary" @click="onDeliver" :loading="loading">确认送达</u-button>
    </view>
  </view>
</template>

<script>
import { deliverTask, getCurrentTask } from '@/api/task'
import { updateLocation } from '@/api/location'

export default {
  data() {
    return {
      taskId: '',
      task: {},
      loading: false,
      locationTimer: null
    }
  },
  computed: {
    statusText() {
      const map = { delivering: '配送中', completed: '已完成', cancelled: '已取消' }
      return map[this.task.status] || '未知'
    },
    statusType() {
      const map = { delivering: 'warning', completed: 'success', cancelled: 'error' }
      return map[this.task.status] || 'info'
    }
  },
  onLoad(options) {
    this.taskId = options.id
    this.loadTask()
  },
  onUnload() {
    this.stopLocationReport()
  },
  methods: {
    async loadTask() {
      try {
        this.task = await getCurrentTask()
        if (this.task.status === 'delivering') {
          this.startLocationReport()
        }
      } catch (e) {
        console.error(e)
      }
    },
    async onDeliver() {
      this.loading = true
      try {
        await deliverTask(this.taskId)
        uni.showToast({ title: '已确认送达' })
        this.stopLocationReport()
        this.$store.commit('SET_CURRENT_TASK', null)
        setTimeout(() => { uni.navigateBack() }, 1000)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    openLocation(lat, lng, name) {
      uni.openLocation({
        latitude: Number(lat),
        longitude: Number(lng),
        name: name
      })
    },
    callPhone(phone) {
      uni.makePhoneCall({ phoneNumber: phone })
    },
    startLocationReport() {
      this.locationTimer = setInterval(() => {
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
      }, 15000)
    },
    stopLocationReport() {
      if (this.locationTimer) {
        clearInterval(this.locationTimer)
        this.locationTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container { padding: 20rpx; }
.task-card {
  background: #fff; border-radius: 16rpx; padding: 30rpx;
  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 24rpx; padding-bottom: 20rpx; border-bottom: 1rpx solid #eee;
    .fee { font-size: 36rpx; font-weight: bold; color: #ff6b00; }
  }
  .info-section {
    margin-bottom: 24rpx;
    .section-label { font-size: 26rpx; color: #999; margin-bottom: 12rpx; }
    .info-row {
      display: flex; align-items: center; margin-bottom: 12rpx; font-size: 28rpx;
      text { margin-left: 12rpx; }
      .link { color: #1890ff; }
    }
    .remark { font-size: 28rpx; color: #666; }
  }
}
.action-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  padding: 20rpx 30rpx; background: #fff;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}
</style>
