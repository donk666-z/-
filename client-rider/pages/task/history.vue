<template>
  <view class="container">
    <view class="history-list">
      <view class="history-item" v-for="item in historyList" :key="item.id">
        <view class="item-header">
          <text class="date">{{ item.completedTime }}</text>
          <text class="earnings">+¥{{ formatMoney(item.deliveryFee) }}</text>
        </view>
        <view class="item-body">
          <view class="merchant">
            <u-icon name="shop" size="26"></u-icon>
            <text>{{ item.merchantName }}</text>
          </view>
          <view class="address">
            <u-icon name="map-fill" size="26" color="#1890ff"></u-icon>
            <text>{{ item.deliveryAddress }}</text>
          </view>
        </view>
      </view>
    </view>

    <u-empty v-if="historyList.length === 0" text="暂无配送记录" mode="list"></u-empty>
  </view>
</template>

<script>
import { getHistory } from '@/api/task'
import { ensureLogin } from '@/utils/session'

export default {
  data() {
    return {
      historyList: []
    }
  },
  onShow() {
    if (!ensureLogin()) {
      return
    }
    this.loadHistory()
  },
  methods: {
    formatMoney(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    },
    async loadHistory() {
      try {
        this.historyList = await getHistory()
      } catch (error) {
        console.error(error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx;
}

.history-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.date {
  font-size: 24rpx;
  color: #999;
}

.earnings {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b00;
}

.merchant,
.address {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  margin-bottom: 8rpx;
}

.merchant text,
.address text {
  margin-left: 10rpx;
}

.merchant {
  color: #333;
}

.address {
  color: #666;
}
</style>
