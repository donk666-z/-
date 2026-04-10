<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-top">
        <view>
          <text class="hero-tag">{{ copy.heroTag }}</text>
          <text class="hero-title">{{ form.name || copy.emptyShopName }}</text>
          <text class="hero-desc">{{ shopOpen ? copy.heroOpenDesc : copy.heroClosedDesc }}</text>
        </view>
        <view class="hero-mark upload-trigger" @click="chooseLogo">
          <image v-if="form.logo" :src="form.logo" class="hero-logo" mode="aspectFill"></image>
          <text v-else>{{ shopInitial }}</text>
        </view>
      </view>

      <view class="hero-meta">
        <view class="meta-pill">
          <u-icon name="clock" size="24" color="#ff7a00"></u-icon>
          <text>{{ form.businessHours || copy.businessHoursPlaceholder }}</text>
        </view>
        <view class="meta-pill">
          <u-icon name="phone" size="24" color="#ff7a00"></u-icon>
          <text>{{ form.phone || copy.phonePlaceholder }}</text>
        </view>
      </view>
    </view>

    <view class="panel status-panel">
      <view class="panel-header">
        <view>
          <text class="panel-tag">{{ copy.statusTag }}</text>
          <text class="panel-title">{{ copy.statusTitle }}</text>
          <text class="panel-desc">{{ copy.statusDesc }}</text>
        </view>
        <view class="status-side">
          <text :class="['status-badge', shopOpen ? 'open' : 'closed']">{{ statusText }}</text>
          <u-switch
            v-model="shopOpen"
            @change="toggleShopStatus"
            activeColor="#ff7a00"
            inactiveColor="#d6dbe5"
            size="46"
          ></u-switch>
        </view>
      </view>

      <view class="status-note">
        <u-icon name="info-circle" size="24" color="#ff7a00"></u-icon>
        <text>{{ statusNote }}</text>
      </view>
    </view>

    <view class="panel info-panel">
      <view class="panel-header stacked">
        <view>
          <text class="panel-tag">{{ copy.infoTag }}</text>
          <text class="panel-title">{{ copy.infoTitle }}</text>
          <text class="panel-desc">{{ copy.infoDesc }}</text>
        </view>
        <view class="progress-box">
          <view class="progress-label">
            <text>{{ copy.completionLabel }}</text>
            <text>{{ completionPercent }}%</text>
          </view>
          <view class="progress-track">
            <view class="progress-fill" :style="{ width: completionPercent + '%' }"></view>
          </view>
        </view>
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">{{ copy.logoLabel }}</text>
          <text class="field-tip">{{ copy.logoTip }}</text>
        </view>
        <view class="logo-upload" @click="chooseLogo">
          <image v-if="form.logo" :src="form.logo" class="logo-preview" mode="aspectFill"></image>
          <view v-else class="logo-placeholder">
            <u-icon name="camera" size="34" color="#ff7a00"></u-icon>
            <text class="logo-placeholder-text">{{ copy.logoUploadText }}</text>
          </view>
        </view>
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label required">{{ copy.nameLabel }}</text>
          <text class="field-tip">{{ copy.nameTip }}</text>
        </view>
        <input
          v-model="form.name"
          class="text-input"
          maxlength="20"
          :placeholder="copy.nameInputPlaceholder"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">{{ copy.descriptionLabel }}</text>
          <text class="field-tip">{{ copy.descriptionTip }}</text>
        </view>
        <textarea
          v-model="form.description"
          class="textarea-input"
          maxlength="120"
          auto-height
          :placeholder="copy.descriptionInputPlaceholder"
          placeholder-class="input-placeholder"
        ></textarea>
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label required">{{ copy.contactLabel }}</text>
          <text class="field-tip">{{ copy.contactTip }}</text>
        </view>
        <input
          v-model="form.phone"
          class="text-input"
          type="number"
          maxlength="20"
          :placeholder="copy.contactInputPlaceholder"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">{{ copy.hoursLabel }}</text>
          <text class="field-tip">{{ copy.hoursTip }}</text>
        </view>
        <input
          v-model="form.businessHours"
          class="text-input"
          maxlength="30"
          :placeholder="copy.hoursInputPlaceholder"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">{{ copy.addressLabel }}</text>
          <text class="field-tip">{{ copy.addressTip }}</text>
        </view>
        <textarea
          v-model="form.address"
          class="textarea-input short"
          maxlength="120"
          auto-height
          :placeholder="copy.addressInputPlaceholder"
          placeholder-class="input-placeholder"
        ></textarea>
      </view>
    </view>

    <view class="panel tips-panel">
      <text class="tips-title">{{ copy.tipsTitle }}</text>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">{{ copy.tipBusinessHours }}</text>
      </view>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">{{ copy.tipPhone }}</text>
      </view>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">{{ copy.tipDescription }}</text>
      </view>
    </view>

    <view class="action-panel">
      <u-button
        type="primary"
        shape="circle"
        :customStyle="saveButtonStyle"
        :loading="loading"
        @click="handleSave"
      >
        {{ copy.saveButton }}
      </u-button>
      <u-button
        plain
        shape="circle"
        :customStyle="logoutButtonStyle"
        @click="handleLogout"
      >
        {{ copy.logoutButton }}
      </u-button>
    </view>
  </view>
</template>

<script>
import { getShopInfo, updateShopInfo, updateStatus } from '@/api/shop'
import merchantRealtime from '@/utils/merchant-realtime'
import { UPLOAD_URL } from '@/config/api'
import { handleUnauthorized } from '@/utils/session'

export default {
  data() {
    return {
      loading: false,
      statusLoading: false,
      shopOpen: true,
      merchantSnapshot: {},
      copy: {
        heroTag: '\u5546\u6237\u540e\u53f0',
        emptyShopName: '\u8bf7\u5148\u5b8c\u5584\u5e97\u94fa\u4fe1\u606f',
        heroOpenDesc: '\u5f53\u524d\u6b63\u5728\u8425\u4e1a\uff0c\u53ef\u6b63\u5e38\u63a5\u6536\u65b0\u8ba2\u5355\u3002',
        heroClosedDesc: '\u5f53\u524d\u5df2\u6253\u70ca\uff0c\u5b66\u751f\u7aef\u4f1a\u663e\u793a\u4f11\u606f\u4e2d\u3002',
        businessHoursPlaceholder: '\u5f85\u8bbe\u7f6e\u8425\u4e1a\u65f6\u95f4',
        phonePlaceholder: '\u5f85\u586b\u5199\u8054\u7cfb\u7535\u8bdd',
        statusTag: '\u8425\u4e1a\u7ba1\u7406',
        statusTitle: '\u5e97\u94fa\u72b6\u6001',
        statusDesc: '\u5207\u6362\u540e\u4f1a\u540c\u6b65\u5f71\u54cd\u5b66\u751f\u7aef\u5c55\u793a\u548c\u4e0b\u5355\u6d41\u7a0b\u3002',
        infoTag: '\u57fa\u7840\u8d44\u6599',
        infoTitle: '\u5e97\u94fa\u4fe1\u606f',
        infoDesc: '\u4fe1\u606f\u8d8a\u5b8c\u6574\uff0c\u7528\u6237\u8d8a\u5bb9\u6613\u4fe1\u4efb\u5e76\u5b8c\u6210\u4e0b\u5355\u3002',
        completionLabel: '\u8d44\u6599\u5b8c\u6574\u5ea6',
        logoLabel: '\u5e97\u94fa\u5934\u50cf',
        logoTip: '\u5efa\u8bae\u4e0a\u4f20\u6e05\u6670\u65b9\u5f62\u56fe\u7247',
        logoUploadText: '\u4e0a\u4f20\u5e97\u94fa\u5934\u50cf',
        nameLabel: '\u5e97\u94fa\u540d\u79f0',
        nameTip: '\u5c55\u793a\u5728\u9996\u9875\u548c\u8ba2\u5355\u9875',
        nameInputPlaceholder: '\u8bf7\u8f93\u5165\u5e97\u94fa\u540d\u79f0',
        descriptionLabel: '\u5e97\u94fa\u63cf\u8ff0',
        descriptionTip: '\u4e00\u53e5\u8bdd\u4ecb\u7ecd\u53e3\u5473\u6216\u7279\u8272',
        descriptionInputPlaceholder: '\u4f8b\u5982\uff1a\u73b0\u505a\u8f7b\u98df\u3001\u4f4e\u8102\u9ad8\u86cb\u767d\u3001\u652f\u6301\u52a0\u6599\u5b9a\u5236',
        contactLabel: '\u8054\u7cfb\u7535\u8bdd',
        contactTip: '\u4fbf\u4e8e\u7528\u6237\u548c\u9a91\u624b\u8054\u7cfb',
        contactInputPlaceholder: '\u8bf7\u8f93\u5165\u8054\u7cfb\u7535\u8bdd',
        hoursLabel: '\u8425\u4e1a\u65f6\u95f4',
        hoursTip: '\u5efa\u8bae\u683c\u5f0f\uff1a8:00-21:00',
        hoursInputPlaceholder: '\u8bf7\u8f93\u5165\u8425\u4e1a\u65f6\u95f4',
        addressLabel: '\u5e97\u94fa\u5730\u5740',
        addressTip: '\u7528\u4e8e\u8ba2\u5355\u914d\u9001\u548c\u5e97\u94fa\u5c55\u793a',
        addressInputPlaceholder: '\u8bf7\u8f93\u5165\u5e97\u94fa\u8be6\u7ec6\u5730\u5740',
        tipsTitle: '\u7ecf\u8425\u5c0f\u63d0\u793a',
        tipBusinessHours: '\u8425\u4e1a\u65f6\u95f4\u5efa\u8bae\u4e0e\u5b9e\u9645\u63a5\u5355\u65f6\u95f4\u4fdd\u6301\u4e00\u81f4\uff0c\u907f\u514d\u7528\u6237\u4e0b\u5355\u540e\u65e0\u6cd5\u5c65\u7ea6\u3002',
        tipPhone: '\u8054\u7cfb\u7535\u8bdd\u5efa\u8bae\u586b\u5199\u5e38\u7528\u624b\u673a\uff0c\u65b9\u4fbf\u9a91\u624b\u8054\u7cfb\u4e0e\u5f02\u5e38\u8ba2\u5355\u5904\u7406\u3002',
        tipDescription: '\u5e97\u94fa\u63cf\u8ff0\u53ef\u4ee5\u7a81\u51fa\u62db\u724c\u83dc\u54c1\u3001\u53e3\u5473\u98ce\u683c\u548c\u914d\u9001\u4f18\u52bf\u3002',
        saveButton: '\u4fdd\u5b58\u8bbe\u7f6e',
        logoutButton: '\u9000\u51fa\u767b\u5f55',
        statusOpen: '\u8425\u4e1a\u4e2d',
        statusClosed: '\u4f11\u606f\u4e2d',
        statusOpenNote: '\u5f53\u524d\u5e97\u94fa\u5bf9\u7528\u6237\u53ef\u89c1\uff0c\u5546\u54c1\u4f1a\u6b63\u5e38\u5c55\u793a\uff0c\u65b0\u7684\u8ba2\u5355\u4f1a\u8fdb\u5165\u5f85\u63a5\u5355\u5217\u8868\u3002',
        statusClosedNote: '\u6253\u70ca\u540e\u7528\u6237\u4ecd\u53ef\u6d4f\u89c8\u5e97\u94fa\u4fe1\u606f\uff0c\u4f46\u65e0\u6cd5\u7ee7\u7eed\u4e0b\u5355\uff0c\u8bf7\u5728\u6062\u590d\u8425\u4e1a\u65f6\u91cd\u65b0\u5f00\u542f\u3002',
        initialFallback: '\u5e97',
        loginRequired: '\u8bf7\u5148\u767b\u5f55',
        accountDisabled: '\u8d26\u53f7\u5df2\u88ab\u7981\u7528\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55',
        uploadFailed: '\u4e0a\u4f20\u5931\u8d25',
        logoutTitle: '\u9000\u51fa\u767b\u5f55',
        logoutContent: '\u786e\u5b9a\u8981\u9000\u51fa\u5f53\u524d\u5546\u6237\u8d26\u53f7\u5417\uff1f',
        loadFailed: '\u52a0\u8f7d\u5e97\u94fa\u4fe1\u606f\u5931\u8d25',
        switchedOpen: '\u5e97\u94fa\u5df2\u5f00\u59cb\u8425\u4e1a',
        switchedClosed: '\u5e97\u94fa\u5df2\u5207\u6362\u4e3a\u4f11\u606f\u4e2d',
        updateStatusFailed: '\u66f4\u65b0\u5e97\u94fa\u72b6\u6001\u5931\u8d25',
        nameRequired: '\u8bf7\u586b\u5199\u5e97\u94fa\u540d\u79f0',
        phoneRequired: '\u8bf7\u586b\u5199\u8054\u7cfb\u7535\u8bdd',
        phoneInvalid: '\u8bf7\u586b\u5199\u6b63\u786e\u7684 11 \u4f4d\u624b\u673a\u53f7',
        hoursInvalid: '\u8425\u4e1a\u65f6\u95f4\u8bf7\u586b\u5199\u4e3a 8:00-21:00 \u8fd9\u7c7b\u683c\u5f0f',
        saveLog: '\u4fdd\u5b58\u5e97\u94fa\u8bbe\u7f6e\uff0c\u8bf7\u6c42\u53c2\u6570\uff1a',
        saveSuccess: '\u5e97\u94fa\u8bbe\u7f6e\u5df2\u4fdd\u5b58',
        saveFailed: '\u4fdd\u5b58\u5e97\u94fa\u8bbe\u7f6e\u5931\u8d25'
      },
      form: {
        logo: '',
        name: '',
        description: '',
        phone: '',
        businessHours: '',
        address: ''
      }
    }
  },
  computed: {
    statusText() {
      return this.shopOpen ? this.copy.statusOpen : this.copy.statusClosed
    },
    statusNote() {
      return this.shopOpen ? this.copy.statusOpenNote : this.copy.statusClosedNote
    },
    completionPercent() {
      const fields = [
        this.form.logo,
        this.form.name,
        this.form.description,
        this.form.phone,
        this.form.businessHours,
        this.form.address
      ]
      const filledCount = fields.filter((item) => this.safeTrim(item)).length
      return Math.round((filledCount / fields.length) * 100)
    },
    shopInitial() {
      const name = this.safeTrim(this.form.name)
      return name ? name.slice(0, 1) : this.copy.initialFallback
    },
    saveButtonStyle() {
      return {
        height: '92rpx',
        marginBottom: '16rpx',
        border: 'none',
        background: 'linear-gradient(135deg, #ff9736 0%, #ff6b00 100%)',
        boxShadow: '0 16rpx 30rpx rgba(255, 122, 0, 0.22)',
        color: '#ffffff',
        fontSize: '30rpx',
        fontWeight: '700'
      }
    },
    logoutButtonStyle() {
      return {
        height: '92rpx',
        border: '2rpx solid #ffd8bc',
        background: '#fff7ef',
        color: '#ff6b00',
        fontSize: '30rpx',
        fontWeight: '700'
      }
    }
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.loadShopInfo()
  },
  methods: {
    safeTrim(value) {
      return value === undefined || value === null ? '' : String(value).trim()
    },
    normalizeForm(data) {
      return {
        name: this.safeTrim(data && data.name),
        logo: this.safeTrim(data && data.logo),
        description: this.safeTrim(data && data.description),
        phone: this.safeTrim(data && data.phone),
        businessHours: this.normalizeBusinessHours(data && data.businessHours),
        address: this.safeTrim(data && data.address)
      }
    },
    normalizeBusinessHours(value) {
      const text = this.safeTrim(value)
      if (!text) {
        return ''
      }
      const match = text.match(/^(\d{1,2}):(\d{2})\s*[-~]+\s*(\d{1,2}):(\d{2})$/)
      if (!match) {
        return text.replace(/\s+/g, '')
      }
      const startHour = match[1].padStart(2, '0')
      const startMinute = match[2]
      const endHour = match[3].padStart(2, '0')
      const endMinute = match[4]
      return `${startHour}:${startMinute}-${endHour}:${endMinute}`
    },
    chooseLogo() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const tempPath = res.tempFilePaths[0]
          uni.uploadFile({
            url: UPLOAD_URL,
            filePath: tempPath,
            name: 'file',
            header: {
              Authorization: `Bearer ${uni.getStorageSync('token')}`
            },
            success: (uploadRes) => {
              if (uploadRes.statusCode === 401) {
                uni.showToast({ title: this.copy.loginRequired, icon: 'none' })
                handleUnauthorized()
                return
              }
              if (uploadRes.statusCode === 403) {
                uni.showToast({ title: this.copy.accountDisabled, icon: 'none' })
                handleUnauthorized()
                return
              }
              const data = JSON.parse(uploadRes.data || '{}')
              if (data.code === 0) {
                this.form.logo = (data.data && data.data.url) || data.data || ''
                return
              }
              uni.showToast({ title: data.message || this.copy.uploadFailed, icon: 'none' })
            },
            fail: () => {
              uni.showToast({ title: this.copy.uploadFailed, icon: 'none' })
            }
          })
        }
      })
    },
    handleLogout() {
      uni.showModal({
        title: this.copy.logoutTitle,
        content: this.copy.logoutContent,
        success: (res) => {
          if (res.confirm) {
            merchantRealtime.stop()
            this.$store.commit('LOGOUT')
            uni.navigateTo({ url: '/pages/login/index' })
          }
        }
      })
    },
    async loadShopInfo() {
      try {
        const res = await getShopInfo()
        if (res) {
          this.merchantSnapshot = { ...res }
          this.form = this.normalizeForm(res)
          this.shopOpen = res.status === 'open' || res.status === 1 || res.status === true
          this.$store.commit('SET_MERCHANT_INFO', res)
        }
      } catch (error) {
        console.error(this.copy.loadFailed, error)
      }
    },
    async toggleShopStatus(value) {
      const nextValue = !!value
      if (this.statusLoading) {
        return
      }
      this.statusLoading = true
      try {
        await updateStatus({ status: nextValue ? 'open' : 'closed' })
        this.shopOpen = nextValue
        this.merchantSnapshot = {
          ...this.merchantSnapshot,
          status: nextValue ? 'open' : 'closed'
        }
        this.$store.commit('SET_MERCHANT_INFO', {
          ...this.merchantSnapshot,
          ...this.form,
          status: nextValue ? 'open' : 'closed'
        })
        uni.showToast({
          title: nextValue ? this.copy.switchedOpen : this.copy.switchedClosed,
          icon: 'none'
        })
      } catch (error) {
        this.shopOpen = !nextValue
        console.error(this.copy.updateStatusFailed, error)
      } finally {
        this.statusLoading = false
      }
    },
    validateForm() {
      if (!this.safeTrim(this.form.name)) {
        uni.showToast({ title: this.copy.nameRequired, icon: 'none' })
        return false
      }
      if (!this.safeTrim(this.form.phone)) {
        uni.showToast({ title: this.copy.phoneRequired, icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(this.safeTrim(this.form.phone))) {
        uni.showToast({ title: this.copy.phoneInvalid, icon: 'none' })
        return false
      }
      const businessHours = this.safeTrim(this.form.businessHours)
      if (businessHours && !/^(\d{1,2}):(\d{2})\s*[-~]+\s*(\d{1,2}):(\d{2})$/.test(businessHours)) {
        uni.showToast({ title: this.copy.hoursInvalid, icon: 'none' })
        return false
      }
      return true
    },
    buildSubmitPayload() {
      const normalizedForm = this.normalizeForm(this.form)
      return {
        ...normalizedForm,
        status: this.shopOpen ? 'open' : 'closed'
      }
    },
    async handleSave() {
      if (!this.validateForm()) {
        return
      }
      this.loading = true
      try {
        const payload = this.buildSubmitPayload()
        console.log(this.copy.saveLog, payload)
        const latest = await updateShopInfo(payload)
        if (latest) {
          this.merchantSnapshot = { ...latest }
          this.form = this.normalizeForm(latest)
          this.shopOpen = latest.status === 'open' || latest.status === 1 || latest.status === true
          this.$store.commit('SET_MERCHANT_INFO', latest)
        } else {
          await this.loadShopInfo()
        }
        uni.showToast({ title: this.copy.saveSuccess, icon: 'none' })
      } catch (error) {
        console.error(this.copy.saveFailed, error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding: 28rpx 24rpx 260rpx;
  background:
    radial-gradient(circle at top right, rgba(255, 196, 133, 0.3) 0, rgba(255, 196, 133, 0) 38%),
    linear-gradient(180deg, #fffaf4 0%, #f5f7fb 38%, #f3f5f8 100%);
  box-sizing: border-box;
}

.hero-card {
  position: relative;
  overflow: hidden;
  margin-bottom: 20rpx;
  padding: 30rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #1f232b 0%, #353c49 42%, #ff8c2a 160%);
  box-shadow: 0 24rpx 60rpx rgba(34, 31, 54, 0.14);
}

.hero-card::after {
  content: '';
  position: absolute;
  right: -70rpx;
  top: -70rpx;
  width: 220rpx;
  height: 220rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.hero-top {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.92);
  font-size: 22rpx;
  letter-spacing: 2rpx;
}

.hero-title {
  display: block;
  margin-top: 20rpx;
  color: #fff;
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.25;
}

.hero-desc {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 24rpx;
  line-height: 1.6;
}

.hero-mark {
  flex-shrink: 0;
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.26) 0%, rgba(255, 255, 255, 0.1) 100%);
  color: #fff;
  font-size: 42rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8rpx);
}

.upload-trigger {
  overflow: hidden;
}

.hero-logo {
  width: 100%;
  height: 100%;
  border-radius: inherit;
}

.hero-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 26rpx;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 14rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.9);
  color: #4a4f59;
  font-size: 24rpx;
}

.panel {
  margin-bottom: 20rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16rpx 36rpx rgba(31, 35, 43, 0.06);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.panel-header.stacked {
  display: block;
}

.panel-tag {
  display: block;
  color: #ff7a00;
  font-size: 22rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.panel-title {
  display: block;
  margin-top: 10rpx;
  color: #1f232b;
  font-size: 34rpx;
  font-weight: 700;
}

.panel-desc {
  display: block;
  margin-top: 10rpx;
  color: #8a909c;
  font-size: 24rpx;
  line-height: 1.6;
}

.status-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16rpx;
}

.status-badge {
  min-width: 140rpx;
  height: 54rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.status-badge.open {
  color: #0f9f5a;
  background: rgba(15, 159, 90, 0.12);
}

.status-badge.closed {
  color: #7f8794;
  background: #eef1f5;
}

.status-note {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: #fff5e9;
  color: #80511b;
  font-size: 24rpx;
  line-height: 1.7;
}

.progress-box {
  margin-top: 22rpx;
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: #f6f8fb;
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #59606d;
  font-size: 24rpx;
}

.progress-track {
  margin-top: 14rpx;
  width: 100%;
  height: 12rpx;
  border-radius: 999rpx;
  background: #e4e8ef;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ffb347 0%, #ff7a00 100%);
}

.field-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #f8fafc;
  border: 1rpx solid #edf1f6;
}

.logo-upload {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180rpx;
  height: 180rpx;
  border-radius: 24rpx;
  border: 2rpx dashed #ffd2af;
  background: #fff7ef;
  overflow: hidden;
}

.logo-preview {
  width: 100%;
  height: 100%;
}

.logo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  color: #ff7a00;
}

.logo-placeholder-text {
  font-size: 24rpx;
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.field-label {
  color: #1f232b;
  font-size: 28rpx;
  font-weight: 700;
}

.field-label.required::before {
  content: '*';
  margin-right: 6rpx;
  color: #ff5a3c;
}

.field-tip {
  color: #9aa3af;
  font-size: 22rpx;
  text-align: right;
}

.text-input,
.textarea-input {
  width: 100%;
  min-height: 92rpx;
  padding: 0 24rpx;
  border-radius: 20rpx;
  background: #fff;
  box-sizing: border-box;
  color: #2b2f36;
  font-size: 28rpx;
  border: 2rpx solid #edf1f6;
}

.text-input {
  height: 92rpx;
  line-height: 92rpx;
}

.textarea-input {
  padding-top: 22rpx;
  padding-bottom: 22rpx;
  line-height: 1.7;
}

.textarea-input.short {
  min-height: 140rpx;
}

.input-placeholder {
  color: #b5bcc8;
}

.tips-title {
  display: block;
  color: #1f232b;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.tip-row {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 10rpx 0;
}

.tip-dot {
  flex-shrink: 0;
  width: 12rpx;
  height: 12rpx;
  margin-top: 12rpx;
  border-radius: 50%;
  background: #ff7a00;
}

.tip-text {
  color: #6c7482;
  font-size: 24rpx;
  line-height: 1.7;
}

.action-panel {
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16rpx 36rpx rgba(31, 35, 43, 0.06);
}
</style>
