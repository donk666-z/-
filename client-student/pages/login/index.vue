<template>
  <view class="container">
    <view class="logo-section">
      <image class="logo" src="/static/logo.png" mode="aspectFit"></image>
      <text class="app-name">校园外卖</text>
    </view>

    <view class="login-section">
      <button type="primary" @click="handleWxLogin" :loading="loading">
        微信一键登录
      </button>
    </view>
  </view>
</template>

<script>
import { wxLogin } from '@/api/auth'

export default {
  data() {
    return { loading: false }
  },
  methods: {
    handleWxLogin() {
      this.loading = true
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            if (!loginRes || !loginRes.code) {
              uni.showToast({
                title: 'uni.login 未拿到 code',
                icon: 'none'
              })
              return
            }
            // 给你一个“拿到了 code”的明确反馈，方便区分问题出在 uni.login 还是 wx-login 接口
            uni.showToast({ title: '已获取微信 code', icon: 'none', duration: 800 })
            const res = await wxLogin(loginRes.code)
            this.$store.dispatch('login', {
              token: res.token,
              userInfo: res.userInfo
            })
            uni.showToast({ title: '登录成功' })
            setTimeout(() => {
              uni.redirectTo({ url: '/pages/index/index' })
            }, 1000)
          } catch (e) {
            // request.js reject 的通常是 res.data: { code, message, data }
            let msg = '登录失败'
            if (typeof e === 'string') {
              msg = e
            } else if (e && typeof e === 'object') {
              const eMsg =
                e.message ||
                e.errMsg ||
                (e.data && (e.data.message || e.data.msg)) ||
                (typeof e.data === 'string' ? e.data : '') ||
                ''
              msg = eMsg || JSON.stringify(e)
            }
            if (msg && msg.length > 50) msg = msg.slice(0, 50) + '...'
            uni.showToast({ title: msg || '登录失败', icon: 'none', duration: 2500 })
            console.error('wx-login error:', e)
          } finally {
            this.loading = false
          }
        },
        fail: (err) => {
          this.loading = false
          const msg = err && (err.errMsg || err.message) ? (err.errMsg || err.message) : 'uni.login 失败'
          uni.showToast({
            title: msg && msg.length > 50 ? (msg.slice(0, 50) + '...') : msg,
            icon: 'none',
            duration: 2500
          })
          console.error('uni.login fail:', err)
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff6b00, #ff9900);
  padding: 200rpx 60rpx 0;
}
.logo-section {
  text-align: center; margin-bottom: 100rpx;
  .logo { width: 200rpx; height: 200rpx; margin-bottom: 30rpx; }
  .app-name { display: block; font-size: 48rpx; font-weight: bold; color: #fff; }
}
.login-section {
  button { background: #fff; color: #ff6b00; border-radius: 50rpx; font-size: 32rpx; font-weight: bold; }
}
</style>
