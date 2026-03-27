<template>
  <view class="container">
    <u-tabs :list="tabs" @change="onTabChange" :current="currentTab"></u-tabs>

    <view class="loading-wrap" v-if="loading">
      <u-loading-icon></u-loading-icon>
    </view>

    <view class="empty-wrap" v-else-if="orderList.length === 0">
      <u-icon name="order" size="60" color="#ccc"></u-icon>
      <text class="empty-text">暂无订单</text>
    </view>

    <view class="order-list" v-else>
      <view class="order-item" v-for="order in orderList" :key="order.id" @click="goToDetail(order.id)">
        <view class="order-header">
          <text class="merchant-name">{{ order.merchantName }}</text>
          <text class="order-status" :style="{ color: getStatusColor(order.status) }">{{ getStatusText(order.status) }}</text>
        </view>
        <view class="order-dishes">
          <view class="dish-item" v-for="(dish, index) in order.dishes" :key="`${dish.id || 'dish'}-${index}`">
            <text>{{ dish.name }} x{{ dish.quantity }}</text>
            <text v-if="dish.type === 'combo' && Array.isArray(dish.comboGroups)" class="dish-summary">{{ getComboSummary(dish.comboGroups) }}</text>
          </view>
        </view>
        <view class="order-time">{{ order.createdAt }}</view>
        <view class="order-footer">
          <text class="order-price">¥{{ order.totalPrice }}</text>
          <view class="order-actions">
            <button v-if="['pending','paid'].includes(order.status)" size="mini" @click.stop="handleCancel(order.id)">取消订单</button>
            <button v-if="order.status === 'delivered'" type="primary" size="mini" @click.stop="handleConfirm(order.id)">确认收货</button>
            <button v-if="order.status === 'completed' && !order.reviewed" type="warn" size="mini" @click.stop="goToReview(order.id)">去评价</button>
            <button v-if="order.status === 'completed'" size="mini" @click.stop="reorder(order)">再来一单</button>
          </view>
        </view>
      </view>
    </view>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { getOrderList, cancelOrder, confirmOrder } from '@/api/order'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      currentTab: 0,
      tabs: [{ name: '待付款' }, { name: '待接单' }, { name: '配送中' }, { name: '待评价' }, { name: '全部' }],
      orderList: [],
      loading: false
    }
  },
  onLoad() {
    this.loadOrderList()
  },
  onShow() {
    const orderTab = this.$store.state.orderTab
    this.currentTab = orderTab !== null && orderTab !== undefined ? orderTab : 4
    this.$store.commit('SET_ORDER_TAB', null)
    this.loadOrderList()
  },
  onPullDownRefresh() {
    this.loadOrderList().then(() => uni.stopPullDownRefresh())
  },
  methods: {
    async loadOrderList() {
      this.loading = true
      try {
        const statusMap = ['pending', 'paid', 'delivering', 'completed', '']
        this.orderList = await getOrderList({ status: statusMap[this.currentTab] })
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    onTabChange(item) {
      this.currentTab = item.index
      this.loadOrderList()
    },
    getComboSummary(comboGroups) {
      return (Array.isArray(comboGroups) ? comboGroups : [])
        .filter((group) => Array.isArray(group.options) && group.options.length > 0)
        .map((group) => `${group.name || '已选'}：${group.options.map((option) => option.dishName).join('、')}`)
        .join('；')
    },
    goToDetail(id) {
      uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
    },
    goToReview(orderId) {
      uni.navigateTo({ url: `/pages/order/review?orderId=${orderId}` })
    },
    async handleCancel(id) {
      uni.showModal({
        title: '提示',
        content: '确定取消该订单吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelOrder(id)
              uni.showToast({ title: '订单已取消', icon: 'none' })
              this.loadOrderList()
            } catch (error) {
              console.error(error)
            }
          }
        }
      })
    },
    async handleConfirm(id) {
      try {
        await confirmOrder(id)
        uni.showToast({ title: '已确认收货', icon: 'none' })
        this.loadOrderList()
      } catch (error) {
        console.error(error)
      }
    },
    reorder(order) {
      uni.navigateTo({ url: `/pages/merchant/detail?id=${order.merchantId}` })
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
    },
    getStatusColor(status) {
      const map = {
        pending: '#ff9900',
        paid: '#ff6b00',
        accepted: '#19be6b',
        prepared: '#19be6b',
        delivering: '#2979ff',
        delivered: '#2979ff',
        completed: '#999',
        cancelled: '#999'
      }
      return map[status] || '#333'
    }
  }
}
</script>

<style lang="scss" scoped>
.container { background: #f8f8f8; min-height: 100vh; padding-bottom: 120rpx; }
.loading-wrap { display: flex; justify-content: center; padding: 100rpx 0; }
.empty-wrap { display: flex; flex-direction: column; align-items: center; padding: 100rpx 0; }
.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
.order-list { padding: 20rpx; }
.order-item { background: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 20rpx; }
.order-header, .order-footer { display: flex; justify-content: space-between; align-items: center; }
.order-header { margin-bottom: 16rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0; }
.merchant-name { font-size: 28rpx; font-weight: bold; }
.order-status { font-size: 24rpx; }
.order-dishes { margin-bottom: 12rpx; }
.dish-item { font-size: 26rpx; color: #666; margin-bottom: 8rpx; }
.dish-summary { display: block; margin-top: 6rpx; font-size: 22rpx; color: #8a8f99; line-height: 1.5; }
.order-time { font-size: 24rpx; color: #bbb; margin-bottom: 16rpx; }
.order-price { font-size: 32rpx; color: #ff6b00; font-weight: bold; }
.order-actions { display: flex; gap: 10rpx; }
.order-actions button { margin-left: 10rpx; font-size: 24rpx; }
</style>
