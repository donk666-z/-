<template>
  <view class="page">
    <view class="hero-bg"></view>

    <view class="brand">
    <view class="brand-mark">
        <u-icon name="account" size="42" color="#1f7ae0"></u-icon>
      </view>
      <view class="brand-copy">
        <text class="brand-title">校园外卖骑手端</text>
        <text class="brand-desc">登录后即可接单、查看统计和管理骑手资料。</text>
      </view>
    </view>

    <view class="card">
      <view class="card-head">
        <text class="card-title">{{ mode === 'login' ? '骑手登录' : '骑手注册' }}</text>
        <text class="card-subtitle">{{ mode === 'login' ? '使用手机号和密码登录骑手端。' : '填写基础资料后即可创建骑手账号。' }}</text>
      </view>

      <view class="mode-tabs">
        <view :class="['mode-tab', mode === 'login' ? 'active' : '']" @click="setMode('login')">登录</view>
        <view :class="['mode-tab', mode === 'register' ? 'active' : '']" @click="setMode('register')">注册</view>
      </view>

      <view v-if="mode === 'register'" class="field-block">
        <text class="field-label">姓名</text>
        <input
          v-model="form.name"
          class="field-input"
          maxlength="12"
          placeholder="请输入骑手姓名"
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

      <view class="tips-box">
        <view class="tips-dot"></view>
        <text class="tips-text">{{ mode === 'login' ? '新骑手还没有账号时，可以先切换到注册页完成开户。' : '注册成功后会自动登录，并默认处于离线状态。' }}</text>
      </view>

      <button class="submit-btn" :loading="loading" @click="handleSubmit">
        {{ mode === 'login' ? '登录进入骑手端' : '注册并进入骑手端' }}
      </button>

      <view class="helper-row">
        <text class="helper-text">{{ mode === 'login' ? '还没有骑手账号？' : '已经有账号了？' }}</text>
        <text class="helper-link" @click="setMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? '立即注册' : '返回登录' }}
        </text>
      </view>
    </view>
  </view>
</template>

<script>
import { riderLogin, riderRegister } from '@/api/auth'
import { getRiderProfile } from '@/api/profile'
import { clearSession, isRiderToken } from '@/utils/session'

export default {
  data() {
    return {
      mode: 'login',
      loading: false,
      form: {
        name: '',
        phone: '',
        password: ''
      }
    }
  },
  onShow() {
    if (isRiderToken()) {
      uni.switchTab({ url: '/pages/index/index' })
      return
    }
    if (uni.getStorageSync('token')) {
      clearSession()
    }
  },
  methods: {
    setMode(mode) {
      if (this.loading) return
      this.mode = mode
    },
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    validateForm() {
      const phone = this.safeTrim(this.form.phone)
      const password = this.safeTrim(this.form.password)
      const name = this.safeTrim(this.form.name)

      if (this.mode === 'register' && !name) {
        uni.showToast({ title: '请输入骑手姓名', icon: 'none' })
        return false
      }
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
        uni.showToast({ title: '密码至少需要 6 位', icon: 'none' })
        return false
      }
      return true
    },
    async finishLogin(res, successText) {
      const safeUserInfo = {
        ...(res.userInfo || {})
      }
      delete safeUserInfo.password

      this.$store.dispatch('login', {
        token: res.token,
        userInfo: safeUserInfo,
        riderStatus: 'offline'
      })

      let profile = null
      try {
        profile = await getRiderProfile()
      } catch (error) {
        profile = null
      }

      const mergedUserInfo = {
        ...safeUserInfo,
        nickname: (profile && profile.name) || safeUserInfo.nickname || '',
        phone: (profile && profile.phone) || safeUserInfo.phone || '',
        avatar: (profile && profile.avatar) || safeUserInfo.avatar || ''
      }

      this.$store.commit('SET_USER_INFO', mergedUserInfo)
      this.$store.commit(
        'SET_RIDER_STATUS',
        (profile && profile.status) || 'offline'
      )

      this.$store.dispatch('login', {
        token: this.$store.state.token,
        userInfo: mergedUserInfo,
        riderStatus: (profile && profile.status) || 'offline'
      })

      uni.showToast({ title: successText, icon: 'none' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
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

        let res
        if (this.mode === 'login') {
          res = await riderLogin(payload)
          await this.finishLogin(res, '登录成功')
        } else {
          res = await riderRegister({
            ...payload,
            name: this.safeTrim(this.form.name)
          })
          await this.finishLogin(res, '注册成功')
        }
      } catch (error) {
        uni.showToast({
          title: (error && error.message) || (this.mode === 'login' ? '登录失败' : '注册失败'),
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 92rpx 28rpx 48rpx;
  box-sizing: border-box;
  background:
    linear-gradient(180deg, #edf5ff 0rpx, #edf5ff 240rpx, #f4f6f8 240rpx, #f4f6f8 100%);
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 240rpx;
  background:
    radial-gradient(circle at top right, rgba(24, 122, 224, 0.28) 0, rgba(24, 122, 224, 0) 34%),
    linear-gradient(135deg, #dcecff 0%, #eef6ff 56%, #f7fbff 100%);
}

.brand,
.card {
  position: relative;
  z-index: 1;
}

.brand {
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
  box-shadow: 0 14rpx 30rpx rgba(31, 122, 224, 0.12);
}

.brand-copy {
  flex: 1;
}

.brand-title {
  display: block;
  color: #1f2430;
  font-size: 40rpx;
  font-weight: 700;
}

.brand-desc {
  display: block;
  margin-top: 8rpx;
  color: #708090;
  font-size: 24rpx;
  line-height: 1.6;
}

.card {
  padding: 34rpx 28rpx 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18rpx 48rpx rgba(20, 31, 51, 0.08);
}

.card-head {
  margin-bottom: 24rpx;
}

.card-title {
  display: block;
  color: #1f2430;
  font-size: 34rpx;
  font-weight: 700;
}

.card-subtitle {
  display: block;
  margin-top: 10rpx;
  color: #8793a2;
  font-size: 24rpx;
  line-height: 1.6;
}

.mode-tabs {
  display: flex;
  padding: 8rpx;
  border-radius: 22rpx;
  background: #f1f5fb;
  margin-bottom: 24rpx;
}

.mode-tab {
  flex: 1;
  height: 76rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7a8694;
  font-size: 28rpx;
  transition: all 0.2s ease;
}

.mode-tab.active {
  color: #1f7ae0;
  background: #fff;
  box-shadow: 0 8rpx 18rpx rgba(31, 122, 224, 0.12);
}

.field-block {
  margin-bottom: 20rpx;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  color: #495161;
  font-size: 24rpx;
  font-weight: 600;
}

.field-input {
  height: 92rpx;
  padding: 0 24rpx;
  border-radius: 22rpx;
  background: #f7f9fc;
  border: 2rpx solid #e8eef5;
  color: #1f2430;
  font-size: 28rpx;
}

.field-placeholder {
  color: #a7b1bf;
}

.tips-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 10rpx 0 24rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f4f8ff;
  border: 2rpx solid #dbe8ff;
}

.tips-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #1f7ae0;
  flex-shrink: 0;
}

.tips-text {
  flex: 1;
  color: #728195;
  font-size: 22rpx;
  line-height: 1.6;
}

.submit-btn {
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #1f7ae0 0%, #43a1ff 100%);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 16rpx 26rpx rgba(31, 122, 224, 0.2);
}

.helper-row {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
}

.helper-text {
  color: #8a94a3;
  font-size: 24rpx;
}

.helper-link {
  color: #1f7ae0;
  font-size: 24rpx;
  font-weight: 600;
}
</style>
