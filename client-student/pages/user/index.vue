<template>
  <view class="container">
    <view class="user-header" @click="handleHeaderClick">
      <image class="avatar" :src="userInfo && userInfo.avatar ? userInfo.avatar : '/static/default-avatar.png'" mode="aspectFill"></image>
      <view class="user-info">
        <text class="nickname">{{ userInfo ? (userInfo.nickname || text.defaultUser) : text.notLoggedIn }}</text>
        <text class="phone" v-if="userInfo && userInfo.phone">{{ formatPhone(userInfo.phone) }}</text>
        <text class="phone" v-else>{{ text.tapToLogin }}</text>
      </view>
      <u-icon name="arrow-right" size="20" color="rgba(255,255,255,0.8)"></u-icon>
    </view>

    <view class="order-shortcuts">
      <view class="shortcut-item" @click="goOrderList(0)">
        <u-icon name="clock" size="40" color="#ff6b00"></u-icon>
        <text>{{ text.pendingPay }}</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(1)">
        <u-icon name="bag" size="40" color="#ff6b00"></u-icon>
        <text>{{ text.pendingAccept }}</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(2)">
        <u-icon name="car" size="40" color="#ff6b00"></u-icon>
        <text>{{ text.delivering }}</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(3)">
        <u-icon name="checkmark-circle" size="40" color="#ff6b00"></u-icon>
        <text>{{ text.pendingReview }}</text>
      </view>
      <view class="shortcut-item" @click="goOrderList('')">
        <u-icon name="order" size="40" color="#ff6b00"></u-icon>
        <text>{{ text.allOrders }}</text>
      </view>
    </view>

    <view class="menu-section">
      <view class="menu-item" @click="goToPage('/pages/user/address')">
        <u-icon name="map" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">{{ text.address }}</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/coupon/list')">
        <u-icon name="coupon" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">{{ text.coupons }}</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/complaint/list')">
        <u-icon name="chat" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">{{ text.feedback }}</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/about/index')">
        <u-icon name="info-circle" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">{{ text.about }}</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
    </view>

    <view class="logout-section" v-if="isLoggedIn">
      <button class="logout-btn" @click="handleLogout">{{ text.logout }}</button>
    </view>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { getProfile } from '@/api/user'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      text: {
        defaultUser: '\u7528\u6237',
        notLoggedIn: '\u672a\u767b\u5f55',
        tapToLogin: '\u70b9\u51fb\u767b\u5f55',
        pendingPay: '\u5f85\u4ed8\u6b3e',
        pendingAccept: '\u5f85\u63a5\u5355',
        delivering: '\u914d\u9001\u4e2d',
        pendingReview: '\u5f85\u8bc4\u4ef7',
        allOrders: '\u5168\u90e8\u8ba2\u5355',
        address: '\u6536\u8d27\u5730\u5740',
        coupons: '\u4f18\u60e0\u5238',
        feedback: '\u6295\u8bc9\u5efa\u8bae',
        about: '\u5173\u4e8e\u6211\u4eec',
        logout: '\u9000\u51fa\u767b\u5f55',
        modalTitle: '\u63d0\u793a',
        modalContent: '\u786e\u5b9a\u8981\u9000\u51fa\u767b\u5f55\u5417\uff1f',
        logoutSuccess: '\u5df2\u9000\u51fa\u767b\u5f55'
      }
    }
  },
  computed: {
    ...mapState(['userInfo', 'token']),
    ...mapGetters(['isLoggedIn'])
  },
  onShow() {
    if (this.isLoggedIn && !this.userInfo) {
      this.loadProfile()
    }
  },
  methods: {
    async loadProfile() {
      try {
        const info = await getProfile()
        this.$store.commit('SET_USER_INFO', info)
      } catch (error) {
        console.error(error)
      }
    },
    formatPhone(phone) {
      if (!phone || phone.length < 11) return phone
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    handleHeaderClick() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/user/edit' })
    },
    goOrderList(tab) {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      if (tab !== '') {
        this.$store.commit('SET_ORDER_TAB', tab)
      } else {
        this.$store.commit('SET_ORDER_TAB', null)
      }
      uni.redirectTo({ url: '/pages/order/list' })
    },
    goToPage(url) {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url })
    },
    handleLogout() {
      uni.showModal({
        title: this.text.modalTitle,
        content: this.text.modalContent,
        success: (res) => {
          if (res.confirm) {
            this.$store.dispatch('logout')
            uni.showToast({ title: this.text.logoutSuccess, icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  background: #f8f8f8;
  min-height: 100vh;
  padding-bottom: 120rpx;
}

.user-header {
  background: linear-gradient(135deg, #ff6b00, #ff9900);
  padding: 60rpx 30rpx;
  display: flex;
  align-items: center;

  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    margin-right: 30rpx;
    border: 4rpx solid rgba(255,255,255,0.3);
  }

  .user-info {
    flex: 1;

    .nickname {
      display: block;
      font-size: 36rpx;
      font-weight: bold;
      color: #fff;
      margin-bottom: 10rpx;
    }

    .phone {
      font-size: 24rpx;
      color: rgba(255,255,255,0.8);
    }
  }
}

.order-shortcuts {
  background: #fff;
  display: flex;
  justify-content: space-around;
  padding: 30rpx 0;
  margin-bottom: 20rpx;

  .shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10rpx;

    text {
      font-size: 24rpx;
      color: #333;
    }
  }
}

.menu-section {
  background: #fff;

  .menu-item {
    display: flex;
    align-items: center;
    padding: 30rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .menu-text {
      flex: 1;
      margin-left: 20rpx;
      font-size: 28rpx;
    }
  }
}

.logout-section {
  padding: 30rpx;
  margin-top: 40rpx;

  .logout-btn {
    background: #fff;
    color: #ff4d4f;
    border: 1rpx solid #ff4d4f;
    border-radius: 50rpx;
    font-size: 30rpx;
  }
}
</style>
