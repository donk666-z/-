<template>
  <view class="container">
    <view class="status-bar">
      <text class="status-text">{{ statusText(order.status) }}</text>
    </view>

    <view class="section">
      <text class="section-title">订单信息</text>
      <view class="info-row">
        <text class="label">订单号</text>
        <text class="value">{{ order.orderNo }}</text>
      </view>
      <view class="info-row">
        <text class="label">下单时间</text>
        <text class="value">{{ order.createdAt || '暂无时间' }}</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">商品详情</text>
      <view v-for="(dish, index) in order.dishes" :key="`${dish.id || 'dish'}-${index}`" class="dish-row">
        <view class="dish-main">
          <view class="dish-head">
            <text class="dish-name">{{ dish.name }}</text>
            <text v-if="dish.type === 'combo'" class="dish-tag">套餐</text>
          </view>
          <view v-if="dish.type === 'combo' && Array.isArray(dish.comboGroups)" class="combo-lines">
            <text v-for="line in getComboLines(dish.comboGroups)" :key="line" class="combo-line">{{ line }}</text>
          </view>
        </view>
        <text class="dish-qty">x{{ dish.quantity }}</text>
        <text class="dish-price">¥{{ dish.price }}</text>
      </view>
      <view class="total-row">
        <text>合计</text>
        <text class="total-price">¥{{ order.totalPrice }}</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">配送信息</text>
      <view class="info-row">
        <text class="label">收货地址</text>
        <text class="value">{{ order.address }}</text>
      </view>
      <view class="info-row">
        <text class="label">联系电话</text>
        <text class="value phone" @click="callPhone">{{ order.phone }}</text>
      </view>
    </view>

    <view class="actions" v-if="order.status === 'paid' || order.status === 'accepted'">
      <u-button v-if="order.status === 'paid'" type="primary" @click="handleAccept">接单</u-button>
      <u-button v-if="order.status === 'accepted'" type="warning" @click="handlePrepare">出餐</u-button>
    </view>
  </view>
</template>

<script>
import { getOrderDetail, acceptOrder, prepareOrder } from '@/api/order'

export default {
  data() {
    return {
      orderId: '',
      order: { dishes: [] }
    }
  },
  onLoad(options) {
    this.orderId = options.id
    this.loadOrder()
  },
  methods: {
    normalizeStatus(status) {
      return String(status || '').trim().toLowerCase()
    },
    statusText(status) {
      const map = {
        pending: '待支付',
        paid: '待接单',
        accepted: '已接单',
        prepared: '已出餐',
        delivering: '配送中',
        delivered: '已送达',
        completed: '已完成',
        cancelled: '已取消'
      }
      return map[this.normalizeStatus(status)] || status || '未知状态'
    },
    getComboLines(comboGroups) {
      return (Array.isArray(comboGroups) ? comboGroups : [])
        .filter((group) => Array.isArray(group.options) && group.options.length > 0)
        .map((group) => `${group.name || '已选'}：${group.options.map((option) => `${option.dishName} x${option.totalQuantity || option.quantity || 1}`).join('、')}`)
    },
    async loadOrder() {
      try {
        const res = await getOrderDetail(this.orderId)
        this.order = {
          ...(res || {}),
          status: this.normalizeStatus(res && res.status),
          dishes: Array.isArray(res && res.dishes) ? res.dishes : []
        }
      } catch (error) {
        console.error('加载订单详情失败', error)
      }
    },
    async handleAccept() {
      try {
        await acceptOrder(this.orderId)
        uni.showToast({ title: '已接单', icon: 'none' })
        this.loadOrder()
      } catch (error) {
        console.error('接单失败', error)
      }
    },
    async handlePrepare() {
      try {
        await prepareOrder(this.orderId)
        uni.showToast({ title: '已出餐', icon: 'none' })
        this.loadOrder()
      } catch (error) {
        console.error('出餐失败', error)
      }
    },
    callPhone() {
      if (this.order.phone) {
        uni.makePhoneCall({ phoneNumber: this.order.phone })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container { padding: 20rpx; background: #f8f8f8; min-height: 100vh; }
.status-bar { background: #ff6b00; border-radius: 16rpx; padding: 30rpx; margin-bottom: 20rpx; }
.status-text { font-size: 36rpx; color: #fff; font-weight: bold; }
.section { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 20rpx; display: block; }
.info-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 12rpx 0; }
.label { font-size: 28rpx; color: #999; }
.value { font-size: 28rpx; color: #333; text-align: right; flex: 1; }
.phone { color: #ff6b00; }
.dish-row { display: flex; gap: 20rpx; padding: 10rpx 0; }
.dish-main { flex: 1; min-width: 0; }
.dish-head { display: flex; align-items: center; gap: 12rpx; }
.dish-name { font-size: 28rpx; color: #333; }
.dish-tag { padding: 4rpx 10rpx; border-radius: 999rpx; background: #fff1e6; color: #ff7a1a; font-size: 20rpx; }
.combo-lines { margin-top: 8rpx; }
.combo-line { display: block; font-size: 22rpx; color: #8a8f99; line-height: 1.5; }
.dish-qty { width: 80rpx; text-align: center; font-size: 28rpx; color: #999; }
.dish-price { width: 140rpx; text-align: right; font-size: 28rpx; color: #333; }
.total-row { display: flex; justify-content: space-between; padding-top: 16rpx; margin-top: 16rpx; border-top: 1rpx solid #eee; }
.total-price { font-size: 32rpx; color: #ff6b00; font-weight: bold; }
.actions { padding: 30rpx 0; }
</style>
