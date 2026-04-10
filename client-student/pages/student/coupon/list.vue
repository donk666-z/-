<template>
  <view class="container">
    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="loading" class="loading-wrap">
      <u-loading-icon></u-loading-icon>
    </view>

    <template v-else>
      <view v-if="currentTab === 'center'">
        <view class="empty-wrap" v-if="centerList.length === 0">
          <u-icon name="coupon" size="60" color="#ccc"></u-icon>
          <text class="empty-text">{{ text.emptyCenter }}</text>
        </view>

        <view class="coupon-list" v-else>
          <view class="coupon-item" v-for="item in centerList" :key="item.promotionId">
            <view class="coupon-left">
              <text class="coupon-value">{{ formatCenterValue(item) }}</text>
              <text class="coupon-limit">{{ text.limitPrefix }}{{ item.minAmount }}{{ text.limitSuffix }}</text>
            </view>
            <view class="coupon-right">
              <text class="coupon-name">{{ item.promotionName }}</text>
              <text class="coupon-desc">{{ item.couponName }}</text>
              <text class="coupon-desc" v-if="item.description">{{ item.description }}</text>
              <text class="coupon-time">{{ text.expirePrefix }}{{ item.endTime }}</text>
            </view>
            <view class="coupon-action">
              <button
                class="claim-btn"
                :class="{ disabled: item.claimStatus !== 'available' }"
                :disabled="item.claimStatus !== 'available'"
                @click="handleClaim(item)"
              >
                {{ claimStatusText[item.claimStatus] || text.claimNow }}
              </button>
            </view>
          </view>
        </view>
      </view>

      <view v-else>
        <view class="empty-wrap" v-if="couponList.length === 0">
          <u-icon name="coupon" size="60" color="#ccc"></u-icon>
          <text class="empty-text">{{ text.emptyMine }}</text>
        </view>

        <view class="coupon-list" v-else>
          <view class="coupon-item" v-for="coupon in couponList" :key="coupon.id">
            <view class="coupon-left">
              <text class="coupon-value">{{ text.currency }}{{ coupon.value }}</text>
              <text class="coupon-limit">{{ text.limitPrefix }}{{ coupon.minAmount }}{{ text.limitSuffix }}</text>
            </view>
            <view class="coupon-right">
              <text class="coupon-name">{{ coupon.name }}</text>
              <text class="coupon-desc">{{ coupon.description }}</text>
              <text class="coupon-time">{{ text.expirePrefix }}{{ coupon.expireTime }}</text>
            </view>
            <view class="coupon-status" :class="{ used: coupon.status === 'used', expired: coupon.status === 'expired' }">
              {{ myCouponStatusText(coupon.status) }}
            </view>
          </view>
        </view>
      </view>
    </template>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import { claimCoupon, getCouponCenter, getCouponList } from '@/api/coupon'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      text: {
        center: '\u9886\u5238\u4e2d\u5fc3',
        mine: '\u6211\u7684\u4f18\u60e0\u5238',
        emptyCenter: '\u6682\u65e0\u53ef\u9886\u53d6\u6d3b\u52a8',
        emptyMine: '\u6682\u65e0\u4f18\u60e0\u5238',
        limitPrefix: '\u6ee1',
        limitSuffix: '\u53ef\u7528',
        expirePrefix: '\u6709\u6548\u671f\u81f3\uff1a',
        claimNow: '\u7acb\u5373\u9886\u53d6',
        claimed: '\u5df2\u9886\u53d6',
        soldOut: '\u5df2\u62a2\u5149',
        used: '\u5df2\u4f7f\u7528',
        expired: '\u5df2\u8fc7\u671f',
        available: '\u53ef\u7528',
        claimSuccess: '\u9886\u53d6\u6210\u529f',
        currency: '\u00a5'
      },
      tabs: [
        { label: '\u9886\u5238\u4e2d\u5fc3', value: 'center' },
        { label: '\u6211\u7684\u4f18\u60e0\u5238', value: 'mine' }
      ],
      currentTab: 'center',
      couponList: [],
      centerList: [],
      loading: false
    }
  },
  computed: {
    claimStatusText() {
      return {
        available: this.text.claimNow,
        claimed: this.text.claimed,
        sold_out: this.text.soldOut
      }
    }
  },
  onLoad() {
    this.loadAll()
  },
  onPullDownRefresh() {
    this.loadAll().then(() => uni.stopPullDownRefresh())
  },
  methods: {
    formatCenterValue(item) {
      if (item.type === 'discount') {
        return `${item.discount}\u6298`
      }
      return `${this.text.currency}${item.discount}`
    },
    myCouponStatusText(status) {
      if (status === 'used') return this.text.used
      if (status === 'expired') return this.text.expired
      return this.text.available
    },
    async loadAll() {
      this.loading = true
      try {
        const [center, mine] = await Promise.all([
          getCouponCenter(),
          getCouponList()
        ])
        this.centerList = center || []
        this.couponList = mine || []
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async handleClaim(item) {
      if (item.claimStatus !== 'available') return
      try {
        await claimCoupon(item.promotionId)
        uni.showToast({ title: this.text.claimSuccess, icon: 'success' })
        this.loadAll()
      } catch (e) {
        console.error(e)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  background: #f8f8f8;
  min-height: 100vh;
  padding: 20rpx;
  padding-bottom: 120rpx;
}

.tab-bar {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
}

.tab-item.active {
  background: #ff6b00;
  color: #fff;
  font-weight: 600;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-top: 20rpx;
}

.coupon-item {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.coupon-left {
  width: 200rpx;
  background: #ff6b00;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30rpx 0;
}

.coupon-value {
  font-size: 44rpx;
  font-weight: bold;
}

.coupon-limit {
  font-size: 22rpx;
  margin-top: 8rpx;
}

.coupon-right {
  flex: 1;
  padding: 20rpx;
}

.coupon-name {
  font-size: 28rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 8rpx;
}

.coupon-desc {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 8rpx;
}

.coupon-time {
  font-size: 22rpx;
  color: #999;
}

.coupon-status,
.coupon-action {
  width: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.coupon-status {
  font-size: 24rpx;
  color: #19be6b;
}

.coupon-status.used {
  color: #999;
}

.coupon-status.expired {
  color: #ff9900;
}

.claim-btn {
  width: 92rpx;
  line-height: 60rpx;
  height: 60rpx;
  border-radius: 30rpx;
  background: #ff6b00;
  color: #fff;
  font-size: 22rpx;
  padding: 0;
}

.claim-btn.disabled {
  background: #ddd;
  color: #999;
}
</style>
