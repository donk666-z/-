<template>
  <view class="page">
    <view class="page-top"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <u-icon name="shopping-cart" size="42" color="#ff7a00"></u-icon>
      </view>
      <view class="brand-copy">
        <text class="brand-name">校园外卖商户端</text>
        <text class="brand-desc">完成注册后即可进入商户后台，继续完善店铺资料。</text>
      </view>
    </view>

    <view class="register-card">
      <view class="card-head">
        <text class="card-title">商户入驻</text>
        <text class="card-subtitle">先创建账号和店铺名称，后续可以在店铺设置里继续补充描述、地址和营业时间。</text>
      </view>

      <view class="tips-box">
        <view class="tips-dot"></view>
        <text class="tips-text">注册成功后会默认生成一家店铺，并自动登录进入后台。</text>
      </view>

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
          placeholder="请输入不少于 6 位的密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <view class="field-block">
        <text class="field-label">店铺名称</text>
        <input
          v-model="form.storeName"
          class="field-input"
          maxlength="20"
          placeholder="请输入店铺名称"
          placeholder-class="field-placeholder"
        />
      </view>

      <button class="submit-btn" :loading="loading" @click="handleRegister">注册并进入后台</button>

      <view class="helper-row">
        <text class="helper-text">已经有账号了？</text>
        <text class="helper-link" @click="goLogin">立即登录</text>
      </view>
    </view>

    <view class="bottom-note">
      <text>提交注册信息后，将以商户身份进入后台管理页面。</text>
    </view>
  </view>
</template>

<script>
import { merchantRegister } from '@/api/auth'

export default {
  data() {
    return {
      loading: false,
      form: {
        phone: '',
        password: '',
        storeName: ''
      }
    }
  },
  methods: {
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    validateForm() {
      const phone = this.safeTrim(this.form.phone)
      const password = this.safeTrim(this.form.password)
      const storeName = this.safeTrim(this.form.storeName)

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
      if (password.length < 6) {
        uni.showToast({ title: '密码至少需要 6 位', icon: 'none' })
        return false
      }
      if (!storeName) {
        uni.showToast({ title: '请输入店铺名称', icon: 'none' })
        return false
      }
      return true
    },
    handleRegister() {
      if (!this.validateForm()) {
        return
      }
      this.loading = true
      merchantRegister({
        phone: this.safeTrim(this.form.phone),
        password: this.safeTrim(this.form.password),
        storeName: this.safeTrim(this.form.storeName)
      }).then(res => {
        this.$store.dispatch('login', {
          token: res.token,
          userInfo: res.userInfo
        })
        uni.showToast({ title: '注册成功', icon: 'none' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/order/list' })
        }, 800)
      }).catch(error => {
        uni.showToast({
          title: (error && error.message) || '注册失败',
          icon: 'none'
        })
      }).finally(() => {
        this.loading = false
      })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/index' })
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
.register-card,
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

.register-card {
  padding: 34rpx 28rpx 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18rpx 48rpx rgba(27, 31, 37, 0.08);
}

.card-head {
  margin-bottom: 24rpx;
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

.tips-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #fff6ec;
  border: 2rpx solid #ffe5c7;
}

.tips-dot {
  flex-shrink: 0;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ff7a00;
}

.tips-text {
  color: #8a5b22;
  font-size: 23rpx;
  line-height: 1.6;
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
  margin-top: 6rpx;
  border-radius: 22rpx;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 92rpx;
  background: linear-gradient(135deg, #ff9736 0%, #ff7a00 100%);
  box-shadow: 0 14rpx 28rpx rgba(255, 122, 0, 0.18);
}

.submit-btn::after {
  border: none;
}

.helper-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 26rpx;
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
