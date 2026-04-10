<template>
  <view class="page">
    <view class="card">
      <view class="section-title">{{ t.avatar }}</view>
      <view class="avatar-block">
        <image class="avatar" :src="avatarPreview" mode="aspectFill"></image>
        <button class="primary-btn" :disabled="uploading || saving" @click="chooseAvatar">
          {{ uploading ? t.uploading : t.changeAvatar }}
        </button>
        <button class="plain-btn" :disabled="uploading || saving" @click="clearAvatar">
          {{ t.removeAvatar }}
        </button>
      </view>

      <view class="section-title section-gap">{{ t.profile }}</view>
      <view class="field">
        <text class="label">{{ t.nickname }}</text>
        <input
          v-model="form.nickname"
          class="input"
          maxlength="20"
          :placeholder="t.nicknamePlaceholder"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <text class="label">{{ t.phone }}</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          maxlength="11"
          :placeholder="t.phonePlaceholder"
          placeholder-class="placeholder"
        />
      </view>
    </view>

    <view class="footer">
      <button class="save-btn" :loading="saving" :disabled="uploading" @click="saveProfile">
        {{ t.save }}
      </button>
    </view>
  </view>
</template>

<script>
import { getProfile, updateProfile, uploadAvatar } from '@/api/user'

const DEFAULT_AVATAR = '/static/default-avatar.png'

export default {
  data() {
    return {
      saving: false,
      uploading: false,
      form: {
        nickname: '',
        phone: '',
        avatar: ''
      },
      t: {
        avatar: '\u5934\u50cf',
        uploading: '\u4e0a\u4f20\u4e2d...',
        changeAvatar: '\u66f4\u6362\u5934\u50cf',
        removeAvatar: '\u79fb\u9664\u5934\u50cf',
        profile: '\u57fa\u672c\u4fe1\u606f',
        nickname: '\u6635\u79f0',
        nicknamePlaceholder: '\u8bf7\u8f93\u5165\u6635\u79f0',
        phone: '\u624b\u673a\u53f7',
        phonePlaceholder: '\u8bf7\u8f93\u5165\u624b\u673a\u53f7',
        save: '\u4fdd\u5b58',
        nicknameRequired: '\u8bf7\u8f93\u5165\u6635\u79f0',
        phoneRequired: '\u8bf7\u8f93\u5165\u624b\u673a\u53f7',
        phoneInvalid: '\u624b\u673a\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e',
        uploadSuccess: '\u5934\u50cf\u4e0a\u4f20\u6210\u529f',
        saveSuccess: '\u4fdd\u5b58\u6210\u529f'
      }
    }
  },
  computed: {
    avatarPreview() {
      return this.form.avatar || DEFAULT_AVATAR
    }
  },
  onLoad() {
    this.loadProfile()
  },
  methods: {
    async loadProfile() {
      try {
        const profile = await getProfile()
        this.form.nickname = profile && profile.nickname ? profile.nickname : ''
        this.form.phone = profile && profile.phone ? profile.phone : ''
        this.form.avatar = profile && profile.avatar ? profile.avatar : ''
        this.$store.commit('SET_USER_INFO', profile)
      } catch (error) {
        console.error(error)
      }
    },
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    validateForm() {
      const nickname = this.safeTrim(this.form.nickname)
      const phone = this.safeTrim(this.form.phone)
      if (!nickname) {
        uni.showToast({ title: this.t.nicknameRequired, icon: 'none' })
        return false
      }
      if (!phone) {
        uni.showToast({ title: this.t.phoneRequired, icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        uni.showToast({ title: this.t.phoneInvalid, icon: 'none' })
        return false
      }
      return true
    },
    chooseAvatar() {
      if (this.saving || this.uploading) {
        return
      }
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const filePath = res.tempFilePaths && res.tempFilePaths[0]
          if (!filePath) {
            return
          }
          this.uploading = true
          try {
            const avatarUrl = await uploadAvatar(filePath)
            this.form.avatar = avatarUrl
            uni.showToast({ title: this.t.uploadSuccess, icon: 'success' })
          } catch (error) {
            console.error(error)
          } finally {
            this.uploading = false
          }
        }
      })
    },
    clearAvatar() {
      this.form.avatar = ''
    },
    async saveProfile() {
      if (!this.validateForm()) {
        return
      }
      this.saving = true
      try {
        const profile = await updateProfile({
          nickname: this.safeTrim(this.form.nickname),
          phone: this.safeTrim(this.form.phone),
          avatar: this.safeTrim(this.form.avatar)
        })
        this.$store.commit('SET_USER_INFO', profile)
        uni.showToast({ title: this.t.saveSuccess, icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 500)
      } catch (error) {
        console.error(error)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f6f7f9;
  padding: 24rpx;
  box-sizing: border-box;
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 12rpx 32rpx rgba(18, 24, 32, 0.05);
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2329;
}

.section-gap {
  margin-top: 40rpx;
}

.avatar-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 24rpx;
}

.avatar {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: #f1f3f6;
}

.primary-btn,
.plain-btn,
.save-btn {
  border-radius: 20rpx;
  font-size: 28rpx;
}

.primary-btn {
  width: 100%;
  margin-top: 24rpx;
  background: #ff7a00;
  color: #fff;
}

.plain-btn {
  width: 100%;
  margin-top: 16rpx;
  background: #fff4e8;
  color: #ff7a00;
}

.field {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #eef1f4;
}

.field:last-child {
  border-bottom: none;
}

.label {
  width: 140rpx;
  color: #2f343c;
  font-size: 28rpx;
  font-weight: 600;
}

.input {
  flex: 1;
  min-width: 0;
  color: #1f2329;
  font-size: 28rpx;
}

.placeholder {
  color: #a7afba;
}

.footer {
  margin-top: 28rpx;
}

.save-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #ff9736 0%, #ff7a00 100%);
  color: #fff;
  box-shadow: 0 16rpx 30rpx rgba(255, 122, 0, 0.18);
}

.primary-btn::after,
.plain-btn::after,
.save-btn::after {
  border: none;
}
</style>
