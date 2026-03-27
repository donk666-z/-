<template>
  <view class="container">
    <view class="loading-wrap" v-if="loading">
      <u-loading-icon></u-loading-icon>
    </view>

    <view class="empty-wrap" v-else-if="couponList.length === 0">
      <u-icon name="coupon" size="60" color="#ccc"></u-icon>
      <text class="empty-text">暂无优惠券</text>
    </view>

    <view class="coupon-list" v-else>
      <view class="coupon-item" v-for="coupon in couponList" :key="coupon.id">
        <view class="coupon-left">
          <text class="coupon-value">¥{{ coupon.value }}</text>
          <text class="coupon-limit">满{{ coupon.minAmount }}可用</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ coupon.name }}</text>
          <text class="coupon-desc">{{ coupon.description }}</text>
          <text class="coupon-time">有效期至：{{ coupon.expireTime }}</text>
        </view>
        <view class="coupon-status" :class="{ used: coupon.status === 'used', expired: coupon.status === 'expired' }">
          {{ coupon.status === 'used' ? '已使用' : coupon.status === 'expired' ? '已过期' : '可用' }}
        </view>
      </view>
    </view>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import { getCouponList } from '@/api/coupon'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      couponList: [],
      loading: false
    }
  },
  onLoad() {
    this.loadCouponList()
  },
  onPullDownRefresh() {
    this.loadCouponList().then(() => uni.stopPullDownRefresh())
  },
  methods: {
    async loadCouponList() {
      this.loading = true
      try {
        const list = await getCouponList()
        this.couponList = list || []
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container { background: #f8f8f8; min-height: 100vh; padding: 20rpx; padding-bottom: 120rpx; }
.loading-wrap { display: flex; justify-content: center; padding: 100rpx 0; }
.empty-wrap {
  display: flex; flex-direction: column; align-items: center; padding: 100rpx 0;
  .empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
}
.coupon-list { }
.coupon-item {
  display: flex; background: #fff; border-radius: 16rpx; margin-bottom: 20rpx; overflow: hidden;
  .coupon-left {
    width: 200rpx; background: #ff6b00; color: #fff;
    display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30rpx 0;
    .coupon-value { font-size: 48rpx; font-weight: bold; }
    .coupon-limit { font-size: 22rpx; margin-top: 8rpx; }
  }
  .coupon-right {
    flex: 1; padding: 20rpx;
    .coupon-name { font-size: 28rpx; font-weight: bold; display: block; margin-bottom: 8rpx; }
    .coupon-desc { font-size: 24rpx; color: #666; display: block; margin-bottom: 8rpx; }
    .coupon-time { font-size: 22rpx; color: #999; }
  }
  .coupon-status {
    width: 100rpx; display: flex; align-items: center; justify-content: center;
    font-size: 24rpx; color: #19be6b;
    &.used { color: #999; }
    &.expired { color: #ff9900; }
  }
}
</style>
