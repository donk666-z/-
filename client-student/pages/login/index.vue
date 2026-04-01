<template>
  <view class="page">
    <view class="page-top"></view>

    <view class="brand-section">
      <view class="brand-mark">
        <u-icon name="shopping-cart" size="42" color="#ff7a00"></u-icon>
      </view>
      <view class="brand-copy">
        <text class="brand-name">校园外卖学生端</text>
        <text class="brand-desc">登录后即可点餐、查看订单和领取优惠券</text>
      </view>
    </view>

    <view class="login-card">
      <view class="card-head">
        <text class="card-title">{{ mode === 'login' ? '学生登录' : '学生注册' }}</text>
        <text class="card-subtitle">{{ mode === 'login' ? '使用手机号和密码登录学生端。' : '填写信息后即可创建学生账号。' }}</text>
      </view>

      <view class="mode-tabs">
        <view :class="['mode-tab', mode === 'login' ? 'active' : '']" @click="setMode('login')">登录</view>
        <view :class="['mode-tab', mode === 'register' ? 'active' : '']" @click="setMode('register')">注册</view>
      </view>

      <view v-if="mode === 'register'" class="field-block">
        <text class="field-label">昵称（可选）</text>
        <input
          v-model="form.nickname"
          class="field-input"
          maxlength="16"
          placeholder="请输入昵称"
          placeholder-class="field-placeholder"
        />
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
          placeholder="请输入至少 6 位密码"
          placeholder-class="field-placeholder"
        />
      </view>

      <button class="submit-btn primary" :loading="loading" @click="handleSubmit">
        {{ mode === 'login' ? '登录进入学生端' : '注册并进入学生端' }}
      </button>

      <view class="helper-row">
        <text class="helper-text">{{ mode === 'login' ? '还没有账号？' : '已经有账号了？' }}</text>
        <text class="helper-link" @click="setMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '立即注册' : '返回登录' }}
        </text>
      </view>

      <view class="wx-split">
        <view class="line"></view>
        <text class="split-text">或使用微信快捷登录</text>
        <view class="line"></view>
      </view>
      <button class="submit-btn wechat" :loading="loadingWx" @click="handleWxLogin">微信一键登录</button>
    </view>
  </view>
</template>

<script>
import { studentLogin, studentRegister, wxLogin } from '@/api/auth'

export default {
  data() {
    return {
      mode: 'login',
      loading: false,
      loadingWx: false,
      form: {
        nickname: '',
        phone: '',
        password: ''
      }
    }
  },
  methods: {
    setMode(mode) {
      if (this.loading || this.loadingWx) return
      this.mode = mode
    },
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    validateForm() {
      const phone = this.safeTrim(this.form.phone)
      const password = this.safeTrim(this.form.password)
      if (!phone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return false
      }
      if (!password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
      if (password.length < 6) {
        uni.showToast({ title: '密码至少 6 位', icon: 'none' })
        return false
      }
      return true
    },
    finishLogin(res, successText) {
      const safeUserInfo = {
        ...(res.userInfo || {})
      }
      delete safeUserInfo.password

      this.$store.dispatch('login', {
        token: res.token,
        userInfo: safeUserInfo
      })
      uni.showToast({ title: successText, icon: 'none' })
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/index/index' })
      }, 700)
    },
    async handleSubmit() {
      if (!this.validateForm()) {
        return
      }
      this.loading = true
      try {
        const payload = {
          phone: this.safeTrim(this.form.phone),
          password: this.safeTrim(this.form.password)
        }
        if (this.mode === 'login') {
          const res = await studentLogin(payload)
          this.finishLogin(res, '登录成功')
        } else {
          const res = await studentRegister({
            ...payload,
            nickname: this.safeTrim(this.form.nickname)
          })
          this.finishLogin(res, '注册成功')
        }
      } catch (error) {
        uni.showToast({
          title: (error && error.message) || (this.mode === 'login' ? '登录失败' : '注册失败'),
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },
    handleWxLogin() {
      this.loadingWx = true
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            if (!loginRes || !loginRes.code) {
              uni.showToast({ title: '微信登录失败', icon: 'none' })
              return
            }
            const res = await wxLogin(loginRes.code)
            this.finishLogin(res, '登录成功')
          } catch (error) {
            uni.showToast({
              title: (error && error.message) || '微信登录失败',
              icon: 'none'
            })
          } finally {
            this.loadingWx = false
          }
        },
        fail: () => {
          this.loadingWx = false
          uni.showToast({ title: '微信登录失败', icon: 'none' })
        }
      })
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
.login-card {
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
  margin-bottom: 26rpx;
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

.mode-tabs {
  display: flex;
  padding: 8rpx;
  margin-bottom: 28rpx;
  border-radius: 20rpx;
  background: #f4f6f8;
}

.mode-tab {
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

.mode-tab.active {
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

.helper-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 24rpx;
  font-size: 24rpx;
}

.helper-text {
  color: #8b929d;
}

.helper-link {
  color: #ff7a00;
  font-weight: 600;
}

.wx-split {
  margin: 26rpx 0 18rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.line {
  flex: 1;
  height: 2rpx;
  background: #eceff4;
}

.split-text {
  color: #98a0aa;
  font-size: 22rpx;
}
</style>

