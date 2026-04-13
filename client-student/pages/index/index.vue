<template>
  <view class="container">
    <view class="search-bar">
      <u-search placeholder="搜索餐厅或菜品" v-model="keyword" @search="onSearch" @clear="onSearch"></u-search>
    </view>

    <!-- 加载中 -->
    <view class="loading-wrap" v-if="loading">
      <u-loading-icon></u-loading-icon>
    </view>

    <!-- 空状态 -->
    <view class="empty-wrap" v-else-if="merchantList.length === 0">
      <u-icon name="search" size="60" color="#ccc"></u-icon>
      <text class="empty-text">暂无餐厅</text>
    </view>

    <!-- 商户列表 -->
    <view class="merchant-list" v-else>
      <view
        class="merchant-item"
        :class="{ 'is-closed': item.status !== 'open' }"
        v-for="item in merchantList"
        :key="item.id"
        @click="goToMerchant(item)"
      >
        <image class="merchant-logo" :src="item.logo" mode="aspectFill"></image>
        <view class="merchant-info">
          <view class="merchant-name">{{ item.name }}</view>
          <view class="merchant-desc">{{ item.description }}</view>
          <view class="merchant-tags">
            <text class="tag">月售{{ item.monthSales || 0 }}</text>
            <text class="tag">评分{{ item.rating || '-' }}</text>
            <text class="tag">配送费¥{{ formatFee(deliveryFee) }}</text>
          </view>
        </view>
        <view class="merchant-status" :class="item.status === 'open' ? 'open' : 'closed'">
          {{ item.status === 'open' ? '营业中' : '休息中' }}
        </view>
      </view>
    </view>

    <StudentTabBarOverlay active="home" />
  </view>
</template>

<script>
import { getMerchantList } from '@/api/merchant'
import { getDeliveryFee } from '@/api/config'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      keyword: '',
      merchantList: [],
      loading: false,
      deliveryFee: 3
    }
  },
  onLoad() {
    this.loadPageData()
  },
  onPullDownRefresh() {
    this.loadPageData().then(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    async loadPageData() {
      await Promise.all([this.loadMerchantList(), this.loadDeliveryFee()])
    },
    async loadMerchantList() {
      this.loading = true
      try {
        this.merchantList = await getMerchantList({ keyword: this.keyword })
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async loadDeliveryFee() {
      try {
        const value = await getDeliveryFee()
        const fee = Number(value)
        this.deliveryFee = Number.isFinite(fee) ? fee : 3
      } catch (error) {
        console.error(error)
        this.deliveryFee = 3
      }
    },
    formatFee(value) {
      const fee = Number(value || 0)
      return Number.isInteger(fee) ? String(fee) : fee.toFixed(2)
    },
    onSearch() {
      this.loadMerchantList()
    },
    goToMerchant(item) {
      if (item.status !== 'open') {
        uni.showToast({ title: '该餐厅暂未营业', icon: 'none' })
        return
      }
      uni.navigateTo({ url: `/pages/merchant/detail?id=${item.id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.container { padding: 20rpx; padding-bottom: 120rpx; }
.search-bar { margin-bottom: 20rpx; }

.loading-wrap { display: flex; justify-content: center; padding: 100rpx 0; }
.empty-wrap {
  display: flex; flex-direction: column; align-items: center; padding: 100rpx 0;
  .empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
}

.merchant-list {
  .merchant-item {
    display: flex; background: #fff; border-radius: 16rpx;
    padding: 20rpx; margin-bottom: 20rpx;
    &.is-closed { opacity: 0.5; }
    .merchant-logo { width: 120rpx; height: 120rpx; border-radius: 12rpx; margin-right: 20rpx; }
    .merchant-info {
      flex: 1;
      .merchant-name { font-size: 32rpx; font-weight: bold; margin-bottom: 10rpx; }
      .merchant-desc { font-size: 24rpx; color: #999; margin-bottom: 10rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .merchant-tags { .tag { display: inline-block; font-size: 22rpx; color: #666; margin-right: 20rpx; } }
    }
    .merchant-status {
      font-size: 24rpx; white-space: nowrap; align-self: center;
      &.open { color: #19be6b; }
      &.closed { color: #999; }
    }
  }
}
</style>
