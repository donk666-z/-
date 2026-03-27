<template>
  <view class="page">
    <view class="page-top"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <u-icon name="shopping-cart" size="42" color="#ff7a00"></u-icon>
      </view>
      <view class="brand-copy">
        <text class="brand-name">校园外卖商户端</text>
        <text class="brand-desc">登录后即可管理订单、菜品和店铺信息</text>
      </view>
    </view>

    <view class="login-card">
      <view class="card-head">
        <text class="card-title">商户登录</text>
        <text class="card-subtitle">使用账号密码登录，或直接切换微信快捷登录。</text>
      </view>

      <view class="login-tabs">
        <view
          :class="['login-tab', loginType === 'account' ? 'active' : '']"
          @click="setLoginType('account')"
        >
          账号登录
        </view>
        <view
          :class="['login-tab', loginType === 'wx' ? 'active' : '']"
          @click="setLoginType('wx')"
        >
          微信快捷登录
        </view>
      </view>

      <view v-if="loginType === 'account'" class="form-area">
        <view class="field-block">
          <text class="field-label">手机号</text>
          <input
            v-model="form.phone"
            class="field-input"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="field-placeholder"
          />
        </view>

        <view class="field-block">
          <text class="field-label">密码</text>
          <input
            v-model="form.password"
            class="field-input"
            password
            maxlength="32"
            placeholder="请输入密码"
            placeholder-class="field-placeholder"
          />
        </view>

        <button class="submit-btn primary" :loading="loading" @click="handleAccountLogin">登录</button>
      </view>

      <view v-else class="wechat-area">
        <view class="wechat-tip-card">
          <view class="wechat-badge">微</view>
          <view class="wechat-tip-copy">
            <text class="wechat-tip-title">微信快捷登录</text>
            <text class="wechat-tip-desc">适合直接在小程序中快速进入商户后台，无需重复输入账号密码。</text>
          </view>
        </view>
        <button class="submit-btn wechat" :loading="loading" @click="handleWxLogin">微信一键登录</button>
      </view>

      <view class="helper-row">
        <text class="helper-text">还没有商户账号？</text>
        <text class="helper-link" @click="goRegister">立即注册</text>
      </view>
    </view>

    <view class="bottom-note">
      <text>登录即表示你将使用商户身份进入后台管理页面。</text>
    </view>
  </view>
</template>

<script>
import { wxLogin, merchantLogin } from '@/api/auth'
import merchantRealtime from '@/utils/merchant-realtime'

export default {
  data() {
    return {
      loading: false,
      loginType: 'account',
      form: {
        phone: '',
        password: ''
      }
    }
  },
  methods: {
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    setLoginType(type) {
      if (this.loading) {
        return
      }
      this.loginType = type
    },
    validateAccountForm() {
      const phone = this.safeTrim(this.form.phone)
      const password = this.safeTrim(this.form.password)

      if (!phone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        uni.showToast({ title: '手机号格式错误', icon: 'none' })
        return false
      }
      if (!password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
      return true
    },
    handleAccountLogin() {
      if (!this.validateAccountForm()) {
        return
      }
      this.loading = true
      merchantLogin({
        phone: this.safeTrim(this.form.phone),
        password: this.safeTrim(this.form.password)
      }).then(res => {
        this.finishLogin(res)
      }).catch(error => {
        uni.showToast({
          title: (error && error.message) || '登录失败',
          icon: 'none'
        })
      }).finally(() => {
        this.loading = false
      })
    },
    handleWxLogin() {
      this.loading = true
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const res = await wxLogin(loginRes.code)
            this.finishLogin(res)
          } catch (error) {
            uni.showToast({ title: '登录失败', icon: 'none' })
          } finally {
            this.loading = false
          }
        },
        fail: () => {
          this.loading = false
          uni.showToast({ title: '登录失败', icon: 'none' })
        }
      })
    },
    finishLogin(res) {
      const store = this.$store
      store.dispatch('login', {
        token: res.token,
        userInfo: res.userInfo
      })
      merchantRealtime.start({
        onNewOrder: (payload) => {
          const pendingCount = Number(payload.pendingCount || 0)
          store.commit('SET_PENDING_COUNT', pendingCount)
          uni.showToast({
            title: '收到新订单，请及时处理',
            icon: 'none'
          })
        }
      })
      uni.showToast({ title: '登录成功', icon: 'none' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/order/list' })
      }, 800)
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/register/index' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 88rpx 28rpx 48rpx;
  box-sizing: border-box;
  background:
    linear-gradient(180deg, #fff4e8 0rpx, #fff4e8 220rpx, #f6f7f9 220rpx, #f6f7f9 100%);
}

.page-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 220rpx;
  background:
    radial-gradient(circle at top right, rgba(255, 163, 71, 0.32) 0, rgba(255, 163, 71, 0) 36%),
    linear-gradient(135deg, #ffe7cf 0%, #fff3e4 58%, #fff7ef 100%);
  z-index: 0;
}

.brand-section,
.login-card,
.bottom-note {
  position: relative;
  z-index: 1;
}

.brand-section {
  display: flex;
  align-items: center;
  gap: 22rpx;
  margin-bottom: 28rpx;
}

.brand-mark {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 28rpx rgba(255, 122, 0, 0.12);
}

.brand-copy {
  flex: 1;
}

.brand-name {
  display: block;
  color: #22252b;
  font-size: 40rpx;
  font-weight: 700;
}

.brand-desc {
  display: block;
  margin-top: 8rpx;
  color: #7f8792;
  font-size: 24rpx;
  line-height: 1.6;
}

.login-card {
  padding: 34rpx 28rpx 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18rpx 48rpx rgba(27, 31, 37, 0.08);
}

.card-head {
  margin-bottom: 28rpx;
}

.card-title {
  display: block;
  color: #1f2329;
  font-size: 34rpx;
  font-weight: 700;
}

.card-subtitle {
  display: block;
  margin-top: 10rpx;
  color: #8b929d;
  font-size: 24rpx;
  line-height: 1.6;
}

.login-tabs {
  display: flex;
  padding: 8rpx;
  margin-bottom: 30rpx;
  border-radius: 20rpx;
  background: #f4f6f8;
}

.login-tab {
  flex: 1;
  height: 72rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #626975;
  font-size: 26rpx;
  font-weight: 600;
}

.login-tab.active {
  color: #ff7a00;
  background: #fff;
  box-shadow: 0 10rpx 18rpx rgba(28, 34, 40, 0.06);
}

.field-block {
  margin-bottom: 24rpx;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  color: #30343b;
  font-size: 26rpx;
  font-weight: 600;
}

.field-input {
  width: 100%;
  height: 92rpx;
  padding: 0 24rpx;
  border-radius: 18rpx;
  border: 2rpx solid #ebeff4;
  background: #fbfcfd;
  color: #252931;
  font-size: 28rpx;
  box-sizing: border-box;
}

.field-placeholder {
  color: #b3bac5;
}

.submit-btn {
  width: 100%;
  height: 92rpx;
  border-radius: 22rpx;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 92rpx;
}

.submit-btn.primary {
  color: #fff;
  background: linear-gradient(135deg, #ff9736 0%, #ff7a00 100%);
  box-shadow: 0 14rpx 28rpx rgba(255, 122, 0, 0.18);
}

.submit-btn.wechat {
  color: #fff;
  background: #1aad19;
  box-shadow: 0 14rpx 28rpx rgba(26, 173, 25, 0.18);
}

.submit-btn::after {
  border: none;
}

.wechat-area {
  padding-top: 4rpx;
}

.wechat-tip-card {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 22rpx;
  margin-bottom: 24rpx;
  border-radius: 20rpx;
  background: #f7faf7;
  border: 2rpx solid #e6f4e5;
}

.wechat-badge {
  flex-shrink: 0;
  width: 48rpx;
  height: 48rpx;
  border-radius: 16rpx;
  background: #1aad19;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wechat-tip-copy {
  flex: 1;
}

.wechat-tip-title {
  display: block;
  color: #26302a;
  font-size: 28rpx;
  font-weight: 700;
}

.wechat-tip-desc {
  display: block;
  margin-top: 8rpx;
  color: #7c8681;
  font-size: 24rpx;
  line-height: 1.6;
}

.helper-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 28rpx;
  font-size: 24rpx;
}

.helper-text {
  color: #8b929d;
}

.helper-link {
  color: #ff7a00;
  font-weight: 600;
}

.bottom-note {
  margin-top: 24rpx;
  text-align: center;
  color: #9ba2ad;
  font-size: 22rpx;
  line-height: 1.7;
}
</style>
