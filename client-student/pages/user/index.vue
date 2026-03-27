<template>
  <view class="container">
    <!-- 用户头部 -->
    <view class="user-header" @click="handleHeaderClick">
      <image class="avatar" :src="userInfo && userInfo.avatar ? userInfo.avatar : '/static/default-avatar.png'" mode="aspectFill"></image>
      <view class="user-info">
        <text class="nickname">{{ userInfo ? (userInfo.nickname || '用户') : '未登录' }}</text>
        <text class="phone" v-if="userInfo && userInfo.phone">{{ formatPhone(userInfo.phone) }}</text>
        <text class="phone" v-else>点击登录</text>
      </view>
      <u-icon v-if="!isLoggedIn" name="arrow-right" size="20" color="rgba(255,255,255,0.8)"></u-icon>
    </view>

    <!-- 订单快捷入口 -->
    <view class="order-shortcuts">
      <view class="shortcut-item" @click="goOrderList(0)">
        <u-icon name="clock" size="40" color="#ff6b00"></u-icon>
        <text>待付款</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(1)">
        <u-icon name="bag" size="40" color="#ff6b00"></u-icon>
        <text>待接单</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(2)">
        <u-icon name="car" size="40" color="#ff6b00"></u-icon>
        <text>配送中</text>
      </view>
      <view class="shortcut-item" @click="goOrderList(3)">
        <u-icon name="checkmark-circle" size="40" color="#ff6b00"></u-icon>
        <text>待评价</text>
      </view>
      <view class="shortcut-item" @click="goOrderList('')">
        <u-icon name="order" size="40" color="#ff6b00"></u-icon>
        <text>全部订单</text>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-section">
      <view class="menu-item" @click="goToPage('/pages/user/address')">
        <u-icon name="map" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">收货地址</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/coupon/list')">
        <u-icon name="coupon" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">优惠券</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/complaint/list')">
        <u-icon name="chat" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">投诉建议</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
      <view class="menu-item" @click="goToPage('/pages/student/about/index')">
        <u-icon name="info-circle" size="24" color="#ff6b00"></u-icon>
        <text class="menu-text">关于我们</text>
        <u-icon name="arrow-right" size="20" color="#999"></u-icon>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="isLoggedIn">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
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
      } catch (e) {
        console.error(e)
      }
    },
    formatPhone(phone) {
      if (!phone || phone.length < 11) return phone
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    handleHeaderClick() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
      }
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
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.$store.dispatch('logout')
            uni.showToast({ title: '已退出登录', icon: 'success' })
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
