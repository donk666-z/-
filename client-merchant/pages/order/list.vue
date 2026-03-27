<template>
  <view class="container">
    <u-tabs :list="tabs" :current="currentTab" @click="onTabChange" :activeColor="'#FF6B00'"></u-tabs>

    <scroll-view scroll-y class="order-list" @scrolltolower="loadMore">
      <view v-if="orders.length === 0" class="empty">
        <u-empty text="暂无订单" mode="order"></u-empty>
      </view>
      <view v-for="item in orders" :key="item.id" class="order-card" @click="goDetail(item.id)">
        <view class="order-header">
          <text class="order-no">订单号: {{ item.orderNo }}</text>
          <text class="order-status">{{ statusText(item.status) }}</text>
        </view>
        <view class="order-dishes">
          <view v-for="dish in item.dishes" :key="dish.id" class="dish-row">
            <text class="dish-name">{{ dish.name }}</text>
            <text class="dish-qty">x{{ dish.quantity }}</text>
          </view>
        </view>
        <view class="order-footer">
          <text class="order-time">{{ item.createdAt || item.createTime || '暂无时间' }}</text>
          <text class="order-price">¥{{ item.totalPrice }}</text>
        </view>
        <view class="order-actions">
          <u-button v-if="item.status === 'paid'" type="primary" size="small"
            @click.stop="handleAccept(item.id)">接单</u-button>
          <u-button v-if="item.status === 'accepted'" type="warning" size="small"
            @click.stop="handlePrepare(item.id)">出餐</u-button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getOrderList, acceptOrder, prepareOrder } from '@/api/order'

export default {
  data() {
    return {
      tabs: [
        { name: '待接单' },
        { name: '已接单' },
        { name: '已出餐' },
        { name: '全部' }
      ],
      currentTab: 0,
      orders: [],
      page: 1,
      hasMore: true
    }
  },
  onLoad() {
    uni.$on('merchant:new-order', this.handleNewOrder)
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.refreshList()
  },
  onUnload() {
    uni.$off('merchant:new-order', this.handleNewOrder)
  },
  methods: {
    statusMap() {
      return {
        pending: '待支付',
        paid: '待接单',
        accepted: '已接单',
        prepared: '已出餐',
        delivering: '配送中',
        delivered: '已送达',
        completed: '已完成',
        cancelled: '已取消'
      }
    },
    normalizeStatus(status) {
      return String(status || '').trim().toLowerCase()
    },
    statusText(status) {
      const normalizedStatus = this.normalizeStatus(status)
      return this.statusMap()[normalizedStatus] || status || '未知状态'
    },
    tabToStatus(index) {
      const map = { 0: 'paid', 1: 'accepted', 2: 'prepared' }
      return map[index] || ''
    },
    onTabChange(item) {
      this.currentTab = item.index
      this.refreshList()
    },
    async refreshList() {
      this.page = 1
      this.hasMore = true
      await this.loadOrders()
    },
    async loadOrders() {
      try {
        const params = {}
        const status = this.tabToStatus(this.currentTab)
        if (status) params.status = status
        const res = await getOrderList(params)
        const orderList = Array.isArray(res && res.records) ? res.records : (Array.isArray(res) ? res : [])
        this.orders = orderList.map(order => ({
          ...order,
          status: this.normalizeStatus(order.status),
          dishes: Array.isArray(order.dishes) ? order.dishes : []
        }))
        this.hasMore = false
      } catch (e) {
        console.error('加载订单失败', e)
      }
    },
    loadMore() {
      if (!this.hasMore) return
    },
    handleNewOrder() {
      this.refreshList()
    },
    async handleAccept(id) {
      try {
        await acceptOrder(id)
        uni.showToast({ title: '已接单' })
        this.refreshList()
      } catch (e) {
        console.error('接单失败', e)
      }
    },
    async handlePrepare(id) {
      try {
        await prepareOrder(id)
        uni.showToast({ title: '已出餐' })
        this.refreshList()
      } catch (e) {
        console.error('出餐失败', e)
      }
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f8f8f8;
}
.order-list {
  height: calc(100vh - 88rpx);
  padding: 20rpx;
}
.order-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.order-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
  .order-no { font-size: 26rpx; color: #666; }
  .order-status { font-size: 26rpx; color: #ff6b00; font-weight: bold; }
}
.dish-row {
  display: flex;
  justify-content: space-between;
  padding: 6rpx 0;
  .dish-name { font-size: 28rpx; color: #333; }
  .dish-qty { font-size: 28rpx; color: #999; }
}
.order-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #eee;
  .order-time { font-size: 24rpx; color: #999; }
  .order-price { font-size: 32rpx; color: #ff6b00; font-weight: bold; }
}
.order-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
  gap: 16rpx;
}
.empty {
  padding-top: 200rpx;
}
</style>
