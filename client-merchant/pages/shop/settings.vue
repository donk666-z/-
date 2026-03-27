<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-top">
        <view>
          <text class="hero-tag">商户后台</text>
          <text class="hero-title">{{ form.name || '请先完善店铺信息' }}</text>
          <text class="hero-desc">{{ shopOpen ? '当前正在营业，可正常接收新订单。' : '当前已打烊，学生端会显示休息中。' }}</text>
        </view>
        <view class="hero-mark">{{ shopInitial }}</view>
      </view>

      <view class="hero-meta">
        <view class="meta-pill">
          <u-icon name="clock" size="24" color="#ff7a00"></u-icon>
          <text>{{ form.businessHours || '待设置营业时间' }}</text>
        </view>
        <view class="meta-pill">
          <u-icon name="phone" size="24" color="#ff7a00"></u-icon>
          <text>{{ form.phone || '待填写联系电话' }}</text>
        </view>
      </view>
    </view>

    <view class="panel status-panel">
      <view class="panel-header">
        <view>
          <text class="panel-tag">营业管理</text>
          <text class="panel-title">店铺状态</text>
          <text class="panel-desc">切换后会同步影响学生端展示和下单流程。</text>
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
          <text class="panel-tag">基础资料</text>
          <text class="panel-title">店铺信息</text>
          <text class="panel-desc">信息越完整，用户越容易信任并完成下单。</text>
        </view>
        <view class="progress-box">
          <view class="progress-label">
            <text>资料完整度</text>
            <text>{{ completionPercent }}%</text>
          </view>
          <view class="progress-track">
            <view class="progress-fill" :style="{ width: completionPercent + '%' }"></view>
          </view>
        </view>
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label required">店铺名称</text>
          <text class="field-tip">展示在首页和订单页</text>
        </view>
        <input
          v-model="form.name"
          class="text-input"
          maxlength="20"
          placeholder="请输入店铺名称"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">店铺描述</text>
          <text class="field-tip">一句话介绍口味或特色</text>
        </view>
        <textarea
          v-model="form.description"
          class="textarea-input"
          maxlength="120"
          auto-height
          placeholder="例如：现做轻食、低脂高蛋白、支持加料定制"
          placeholder-class="input-placeholder"
        ></textarea>
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label required">联系电话</text>
          <text class="field-tip">便于用户和骑手联系</text>
        </view>
        <input
          v-model="form.phone"
          class="text-input"
          type="number"
          maxlength="20"
          placeholder="请输入联系电话"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">营业时间</text>
          <text class="field-tip">建议格式：08:00-21:00</text>
        </view>
        <input
          v-model="form.businessHours"
          class="text-input"
          maxlength="30"
          placeholder="请输入营业时间"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field-card">
        <view class="field-head">
          <text class="field-label">店铺地址</text>
          <text class="field-tip">用于订单配送和店铺展示</text>
        </view>
        <textarea
          v-model="form.address"
          class="textarea-input short"
          maxlength="120"
          auto-height
          placeholder="请输入店铺详细地址"
          placeholder-class="input-placeholder"
        ></textarea>
      </view>
    </view>

    <view class="panel tips-panel">
      <text class="tips-title">经营小提示</text>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">营业时间建议与实际接单时间保持一致，避免用户下单后无法履约。</text>
      </view>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">联系电话建议填写常用手机，方便骑手联系与异常订单处理。</text>
      </view>
      <view class="tip-row">
        <text class="tip-dot"></text>
        <text class="tip-text">店铺描述可以突出招牌菜、口味风格和配送优势。</text>
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
        保存设置
      </u-button>
      <u-button
        plain
        shape="circle"
        :customStyle="logoutButtonStyle"
        @click="handleLogout"
      >
        退出登录
      </u-button>
    </view>
  </view>
</template>

<script>
import { getShopInfo, updateShopInfo, updateStatus } from '@/api/shop'

export default {
  data() {
    return {
      loading: false,
      statusLoading: false,
      shopOpen: true,
      merchantSnapshot: {},
      form: {
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
      return this.shopOpen ? '营业中' : '休息中'
    },
    statusNote() {
      return this.shopOpen
        ? '当前店铺对用户可见，商品会正常展示，新的订单会进入待接单列表。'
        : '打烊后用户仍可浏览店铺信息，但无法继续下单，请在恢复营业时重新开启。'
    },
    completionPercent() {
      const fields = [
        this.form.name,
        this.form.description,
        this.form.phone,
        this.form.businessHours,
        this.form.address
      ]
      const filledCount = fields.filter(item => this.safeTrim(item)).length
      return Math.round((filledCount / fields.length) * 100)
    },
    shopInitial() {
      const name = this.safeTrim(this.form.name)
      return name ? name.slice(0, 1) : '店'
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
      const match = text.match(/^(\d{1,2})[:：](\d{2})\s*[-~至到]+\s*(\d{1,2})[:：](\d{2})$/)
      if (!match) {
        return text.replace(/[：]/g, ':').replace(/\s+/g, '')
      }
      const startHour = match[1].padStart(2, '0')
      const startMinute = match[2]
      const endHour = match[3].padStart(2, '0')
      const endMinute = match[4]
      return `${startHour}:${startMinute}-${endHour}:${endMinute}`
    },
    handleLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定要退出当前商户账号吗？',
        success: (res) => {
          if (res.confirm) {
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
        console.error('加载店铺信息失败', error)
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
          title: nextValue ? '店铺已开始营业' : '店铺已切换为休息中',
          icon: 'none'
        })
      } catch (error) {
        this.shopOpen = !nextValue
        console.error('更新店铺状态失败', error)
      } finally {
        this.statusLoading = false
      }
    },
    validateForm() {
      if (!this.safeTrim(this.form.name)) {
        uni.showToast({ title: '请填写店铺名称', icon: 'none' })
        return false
      }
      if (!this.safeTrim(this.form.phone)) {
        uni.showToast({ title: '请填写联系电话', icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(this.safeTrim(this.form.phone))) {
        uni.showToast({ title: '请填写正确的 11 位手机号', icon: 'none' })
        return false
      }
      const businessHours = this.safeTrim(this.form.businessHours)
      if (businessHours && !/^(\d{1,2})[:：](\d{2})\s*[-~至到]+\s*(\d{1,2})[:：](\d{2})$/.test(businessHours)) {
        uni.showToast({ title: '营业时间请填写为 8:00-21:00 这类格式', icon: 'none' })
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
        console.log('保存店铺设置，请求参数：', payload)
        const latest = await updateShopInfo(payload)
        if (latest) {
          this.merchantSnapshot = { ...latest }
          this.form = this.normalizeForm(latest)
          this.shopOpen = latest.status === 'open' || latest.status === 1 || latest.status === true
          this.$store.commit('SET_MERCHANT_INFO', latest)
        } else {
          await this.loadShopInfo()
        }
        uni.showToast({ title: '店铺设置已保存', icon: 'none' })
      } catch (error) {
        console.error('保存店铺设置失败', error)
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
