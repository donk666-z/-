<template>
  <view class="container">
    <view class="card">
      <text class="status-text">{{ getStatusText(order.status) }}</text>
      <text class="status-desc" v-if="order.status === 'delivering' && order.estimatedTime">预计 {{ order.estimatedTime }} 送达</text>
    </view>

    <view class="card" v-if="order.riderName && ['delivering', 'delivered'].includes(order.status)">
      <view class="row">
        <text>骑手：{{ order.riderName }}</text>
        <text class="link" @click="callRider" v-if="order.riderPhone">联系骑手</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">配送信息</text>
      <view class="info-row"><text class="label">收货地址</text><text>{{ order.address }}</text></view>
      <view class="info-row"><text class="label">联系电话</text><text>{{ order.phone }}</text></view>
      <view class="info-row" v-if="order.orderNo"><text class="label">订单编号</text><text>{{ order.orderNo }}</text></view>
      <view class="info-row" v-if="order.createdAt"><text class="label">下单时间</text><text>{{ order.createdAt }}</text></view>
    </view>

    <view class="card">
      <text class="section-title">{{ order.merchantName || '商品明细' }}</text>
      <view class="dish-item" v-for="(dish, index) in order.dishes" :key="`${dish.id || 'dish'}-${index}`">
        <view class="dish-main">
          <view class="dish-head">
            <text class="dish-name">{{ dish.name }}</text>
            <text v-if="dish.type === 'combo'" class="dish-tag">套餐</text>
          </view>
          <view v-if="dish.type === 'combo' && Array.isArray(dish.comboGroups)" class="combo-lines">
            <text v-for="line in getComboLines(dish.comboGroups)" :key="line" class="combo-line">{{ line }}</text>
          </view>
        </view>
        <text class="dish-quantity">x{{ dish.quantity }}</text>
        <text class="dish-price">¥{{ dish.price }}</text>
      </view>
    </view>

    <view class="card">
      <view class="info-row"><text>菜品总价</text><text>¥{{ order.dishPrice }}</text></view>
      <view class="info-row"><text>配送费</text><text>¥{{ order.deliveryFee }}</text></view>
      <view class="info-row total"><text>实付金额</text><text class="total-price">¥{{ order.totalPrice }}</text></view>
    </view>

    <view class="action-bar" v-if="showActions">
      <button v-if="['pending', 'paid'].includes(order.status)" size="mini" @click="handleCancel">取消订单</button>
      <button v-if="order.status === 'delivered'" type="primary" size="mini" @click="handleConfirm">确认收货</button>
      <button v-if="order.status === 'completed' && !order.reviewed" type="warn" size="mini" @click="goReview">去评价</button>
      <button v-if="order.status === 'completed'" size="mini" @click="reorder">再来一单</button>
    </view>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { getOrderDetail, cancelOrder, confirmOrder } from '@/api/order'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      orderId: '',
      order: {}
    }
  },
  computed: {
    showActions() {
      return ['pending', 'paid', 'delivered', 'completed'].includes(this.order.status)
    }
  },
  onLoad(options) {
    this.orderId = options.id
    this.loadOrder()
  },
  methods: {
    async loadOrder() {
      try {
        this.order = await getOrderDetail(this.orderId)
      } catch (error) {
        console.error(error)
      }
    },
    getComboLines(comboGroups) {
      return (Array.isArray(comboGroups) ? comboGroups : [])
        .filter((group) => Array.isArray(group.options) && group.options.length > 0)
        .map((group) => `${group.name || '已选'}：${group.options.map((option) => `${option.dishName} x${option.totalQuantity || option.quantity || 1}`).join('、')}`)
    },
    callRider() {
      if (this.order.riderPhone) {
        uni.makePhoneCall({ phoneNumber: this.order.riderPhone })
      }
    },
    async handleCancel() {
      uni.showModal({
        title: '提示',
        content: '确定取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelOrder(this.orderId)
              uni.showToast({ title: '已取消', icon: 'none' })
              this.loadOrder()
            } catch (error) {
              console.error(error)
            }
          }
        }
      })
    },
    async handleConfirm() {
      try {
        await confirmOrder(this.orderId)
        uni.showToast({ title: '已确认收货', icon: 'none' })
        this.loadOrder()
      } catch (error) {
        console.error(error)
      }
    },
    goReview() {
      uni.navigateTo({ url: `/pages/order/review?orderId=${this.orderId}` })
    },
    reorder() {
      uni.navigateTo({ url: `/pages/merchant/detail?id=${this.order.merchantId}` })
    },
    getStatusText(status) {
      const map = {
        pending: '待支付',
        paid: '待接单',
        accepted: '制作中',
        prepared: '待取餐',
        delivering: '配送中',
        delivered: '已送达',
        completed: '已完成',
        cancelled: '已取消'
      }
      return map[status] || status
    }
  }
}
</script>

<style lang="scss" scoped>
.container { background: #f8f8f8; min-height: 100vh; padding: 20rpx 20rpx 140rpx; }
.card { background: #fff; border-radius: 18rpx; padding: 24rpx; margin-bottom: 20rpx; }
.status-text { display: block; font-size: 36rpx; font-weight: bold; }
.status-desc { display: block; margin-top: 10rpx; font-size: 24rpx; color: #999; }
.section-title { display: block; margin-bottom: 16rpx; font-size: 30rpx; font-weight: bold; }
.row, .info-row, .dish-item { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; }
.info-row { padding: 12rpx 0; }
.label { color: #999; width: 160rpx; flex-shrink: 0; }
.link { color: #ff6b00; }
.dish-item { padding: 14rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.dish-item:last-child { border-bottom: none; }
.dish-main { flex: 1; min-width: 0; }
.dish-head { display: flex; align-items: center; gap: 12rpx; }
.dish-name { font-size: 28rpx; color: #333; }
.dish-tag { padding: 4rpx 10rpx; border-radius: 999rpx; background: #fff1e6; color: #ff7a1a; font-size: 20rpx; }
.combo-lines { margin-top: 8rpx; }
.combo-line { display: block; font-size: 22rpx; color: #8a8f99; line-height: 1.5; }
.dish-quantity { color: #999; }
.dish-price, .total-price { color: #ff6b00; }
.total { padding-top: 12rpx; border-top: 1rpx solid #f0f0f0; font-size: 30rpx; font-weight: bold; }
.action-bar {
  position: fixed;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  height: 100rpx;
  z-index: 1101;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 30rpx;
  gap: 16rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.1);
}
</style>
