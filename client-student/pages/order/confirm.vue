<template>
  <view class="container">
    <view class="address-section" @click="chooseAddress">
      <view v-if="selectedAddress" class="address-card">
        <view class="address-user">
          <text class="name">{{ selectedAddress.name }}</text>
          <text class="phone">{{ selectedAddress.phone }}</text>
        </view>
        <view class="address-detail">{{ formatAddress(selectedAddress) }}</view>
      </view>
      <view v-else class="address-empty">
        <u-icon name="plus-circle" size="24" color="#ff6b00"></u-icon>
        <text>{{ text.chooseAddress }}</text>
      </view>
      <u-icon name="arrow-right" size="20" color="#999"></u-icon>
    </view>

    <view class="merchant-section">
      <text class="merchant-name">{{ merchantName }}</text>
    </view>

    <view class="dish-section">
      <view class="dish-item" v-for="item in cart" :key="item.cartKey || item.dishId">
        <view class="dish-main">
          <text class="dish-name">{{ item.name }}</text>
          <text v-if="item.type === 'combo' && item.comboSummary" class="dish-summary">{{ item.comboSummary }}</text>
        </view>
        <text class="dish-quantity">x{{ item.quantity }}</text>
        <text class="dish-price">{{ text.currency }}{{ formatAmount(item.price * item.quantity) }}</text>
      </view>
    </view>

    <view class="coupon-section" @click="openCouponPopup">
      <text class="label">{{ text.couponLabel }}</text>
      <view class="coupon-right">
        <text v-if="selectedCoupon" class="coupon-value selected">
          {{ selectedCoupon.name }}锛寋{ text.minusPrefix }}{{ formatAmount(discountAmount) }}
        </text>
        <text v-else-if="usableCoupons.length > 0" class="coupon-value">
          {{ text.couponAvailablePrefix }}{{ usableCoupons.length }}{{ text.couponAvailableSuffix }}
        </text>
        <text v-else class="coupon-value disabled">{{ text.noCouponAvailable }}</text>
        <u-icon name="arrow-right" size="18" color="#999"></u-icon>
      </view>
    </view>

    <view class="remark-section">
      <text class="label">{{ text.remark }}</text>
      <input class="remark-input" v-model="remark" :placeholder="text.remarkPlaceholder" />
    </view>

    <view class="price-section">
      <view class="price-row">
        <text>{{ text.dishTotal }}</text>
        <text>{{ text.currency }}{{ dishTotal }}</text>
      </view>
      <view class="price-row">
        <text>{{ text.deliveryFee }}</text>
        <text>{{ text.currency }}{{ formatAmount(deliveryFee) }}</text>
      </view>
      <view class="price-row" v-if="discountAmount > 0">
        <text>{{ text.couponDiscount }}</text>
        <text class="discount-text">-{{ text.currency }}{{ formatAmount(discountAmount) }}</text>
      </view>
      <view class="price-row total">
        <text>{{ text.payAmount }}</text>
        <text class="total-price">{{ text.currency }}{{ totalPrice }}</text>
      </view>
    </view>

    <view class="submit-bar">
      <view class="submit-price">
        <text>{{ text.totalLabel }}</text>
        <text class="price">{{ text.currency }}{{ totalPrice }}</text>
      </view>
      <view class="submit-btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">{{ text.submit }}</view>
    </view>

    <u-popup :show="showCouponPopup" mode="bottom" round="20" @close="closeCouponPopup">
      <view class="popup-card">
        <view class="popup-head">
          <text class="popup-title">{{ text.couponPopupTitle }}</text>
          <text class="popup-link" @click="closeCouponPopup">{{ text.close }}</text>
        </view>
        <view class="coupon-option" :class="{ active: !selectedCouponId }" @click="selectCoupon(null)">
          <view class="coupon-option-main">
            <text class="coupon-option-name">{{ text.noCouponUse }}</text>
            <text class="coupon-option-desc">{{ text.noCouponUseDesc }}</text>
          </view>
          <u-icon v-if="!selectedCouponId" name="checkmark-circle-fill" size="20" color="#ff6b00"></u-icon>
        </view>
        <view v-if="usableCoupons.length === 0" class="empty-coupon">{{ text.noCouponAvailable }}</view>
        <view
          v-for="coupon in usableCoupons"
          :key="coupon.id"
          class="coupon-option"
          :class="{ active: selectedCouponId === coupon.id }"
          @click="selectCoupon(coupon.id)"
        >
          <view class="coupon-option-main">
            <text class="coupon-option-name">{{ coupon.name }}</text>
            <text class="coupon-option-desc">{{ formatCouponDesc(coupon) }}</text>
          </view>
          <view class="coupon-option-side">
            <text class="coupon-option-minus">{{ text.minusPrefix }}{{ formatAmount(getCouponDiscount(coupon)) }}</text>
            <u-icon
              v-if="selectedCouponId === coupon.id"
              name="checkmark-circle-fill"
              size="20"
              color="#ff6b00"
            ></u-icon>
          </view>
        </view>
      </view>
    </u-popup>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import { createOrder } from '@/api/order'
import { getCouponList } from '@/api/coupon'
import { getAddresses } from '@/api/user'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import { getDeliveryFee } from '@/api/config'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      text: {
        chooseAddress: '\u8bf7\u9009\u62e9\u6536\u8d27\u5730\u5740',
        couponLabel: '\u4f18\u60e0\u5238',
        couponAvailablePrefix: '\u53ef\u7528\u4f18\u60e0\u5238 ',
        couponAvailableSuffix: ' \u5f20',
        noCouponAvailable: '\u6682\u65e0\u53ef\u7528\u4f18\u60e0\u5238',
        remark: '\u5907\u6ce8',
        remarkPlaceholder: '\u53e3\u5473\u3001\u504f\u597d\u7b49\uff08\u9009\u586b\uff09',
        dishTotal: '\u83dc\u54c1\u603b\u4ef7',
        deliveryFee: '\u914d\u9001\u8d39',
        couponDiscount: '\u4f18\u60e0\u5238\u62b5\u6263',
        payAmount: '\u5b9e\u4ed8\u91d1\u989d',
        totalLabel: '\u5408\u8ba1\uff1a',
        submit: '\u63d0\u4ea4\u8ba2\u5355',
        submitSuccess: '\u652f\u4ed8\u6210\u529f\uff08\u6a21\u62df\uff09',
        selectAddressFirst: '\u8bf7\u9009\u62e9\u6536\u8d27\u5730\u5740',
        couponPopupTitle: '\u9009\u62e9\u4f18\u60e0\u5238',
        close: '\u5173\u95ed',
        noCouponUse: '\u4e0d\u4f7f\u7528\u4f18\u60e0\u5238',
        noCouponUseDesc: '\u6309\u539f\u4ef7\u7ed3\u7b97',
        minusPrefix: '\u53ef\u51cf ',
        currency: '\u00a5'
      },
      merchantId: '',
      merchantName: '',
      selectedAddress: null,
      remark: '',
      deliveryFee: 0,
      submitting: false,
      couponList: [],
      selectedCouponId: null,
      showCouponPopup: false
    }
  },
  computed: {
    ...mapState(['cart']),
    ...mapGetters(['cartAmount']),
    dishTotal() {
      return this.formatAmount(this.cartAmount)
    },
    usableCoupons() {
      const now = Date.now()
      return (Array.isArray(this.couponList) ? this.couponList : []).filter((coupon) => {
        if (!coupon || coupon.status !== 'unused') return false
        const minAmount = Number(coupon.minAmount || 0)
        if (this.cartAmount < minAmount) return false
        if (coupon.expireTime) {
          const expireTime = new Date(coupon.expireTime).getTime()
          if (!Number.isNaN(expireTime) && expireTime < now) return false
        }
        return this.getCouponDiscount(coupon) > 0
      })
    },
    selectedCoupon() {
      return this.usableCoupons.find((coupon) => coupon.id === this.selectedCouponId) || null
    },
    discountAmount() {
      return this.selectedCoupon ? this.getCouponDiscount(this.selectedCoupon) : 0
    },
    totalPrice() {
      const payable = Math.max(this.cartAmount - this.discountAmount, 0) + Number(this.deliveryFee || 0)
      return this.formatAmount(payable)
    },
    canSubmit() {
      return this.selectedAddress && this.cart.length > 0 && !this.submitting
    }
  },
  onLoad(options) {
    this.merchantId = options.merchantId || ''
    this.merchantName = decodeURIComponent(options.merchantName || '')
    this.loadPageData()
  },
  methods: {
    async loadPageData() {
      await Promise.all([this.loadDefaultAddress(), this.loadCoupons(), this.loadDeliveryFee()])
    },
    async loadDefaultAddress() {
      try {
        const list = await getAddresses()
        this.selectedAddress = list.find((item) => item.isDefault) || list[0] || null
      } catch (error) {
        console.error(error)
      }
    },
    async loadCoupons() {
      try {
        this.couponList = await getCouponList()
        if (this.selectedCouponId && !this.usableCoupons.find((item) => item.id === this.selectedCouponId)) {
          this.selectedCouponId = null
        }
      } catch (error) {
        console.error(error)
        this.couponList = []
      }
    },
    async loadDeliveryFee() {
      try {
        const value = await getDeliveryFee()
        const fee = Number(value)
        this.deliveryFee = Number.isFinite(fee) ? fee : 3
      } catch (error) {
        console.error(error)
        this.deliveryFee = 3
      }
    },
    chooseAddress() {
      uni.navigateTo({
        url: '/pages/user/address?mode=select',
        events: {
          selectAddress: (address) => {
            this.selectedAddress = address
          }
        }
      })
    },
    formatAddress(address) {
      if (!address) return ''
      return [address.address, address.building, address.room].filter(Boolean).join(' ')
    },
    formatAmount(value) {
      const numberValue = Number(value || 0)
      return numberValue.toFixed(2)
    },
    getCouponDiscount(coupon) {
      if (!coupon) return 0
      const dishAmount = Number(this.cartAmount || 0)
      if (dishAmount <= 0) return 0

      if (coupon.type === 'discount') {
        const rate = Number(coupon.value || 0)
        if (rate <= 0) return 0
        const payable = Number((dishAmount * rate / 10).toFixed(2))
        return Number(Math.max(dishAmount - payable, 0).toFixed(2))
      }

      return Number(Math.min(Number(coupon.value || 0), dishAmount).toFixed(2))
    },
    formatCouponDesc(coupon) {
      const amountText = coupon.type === 'discount'
        ? `${coupon.value}\u6298`
        : `${this.text.currency}${this.formatAmount(coupon.value)}`
      return `${amountText}\uff0c\u6ee1${this.formatAmount(coupon.minAmount || 0)}\u53ef\u7528`
    },
    openCouponPopup() {
      this.showCouponPopup = true
    },
    closeCouponPopup() {
      this.showCouponPopup = false
    },
    selectCoupon(couponId) {
      this.selectedCouponId = couponId
      this.closeCouponPopup()
    },
    buildOrderItems() {
      return this.cart.map((item) => ({
        dishId: item.dishId || item.id,
        type: item.type === 'combo' ? 'combo' : 'single',
        name: item.name,
        image: item.image || '',
        price: item.price,
        quantity: item.quantity,
        comboSelections: Array.isArray(item.comboSelections)
          ? item.comboSelections.map((group) => ({
              categoryId: group.categoryId !== undefined && group.categoryId !== null ? Number(group.categoryId) : null,
              categoryName: group.categoryName || group.name || '',
              requiredCount: group.requiredCount !== undefined && group.requiredCount !== null ? Number(group.requiredCount) : 1,
              options: Array.isArray(group.options)
                ? group.options.map((option) => ({
                    dishId: option.dishId,
                    dishName: option.dishName,
                    quantity: option.quantity,
                    extraPrice: option.extraPrice
                  }))
                : []
            }))
          : []
      }))
    },
    async handleSubmit() {
      if (!this.canSubmit) {
        if (!this.selectedAddress) {
          uni.showToast({ title: this.text.selectAddressFirst, icon: 'none' })
        }
        return
      }

      this.submitting = true
      try {
        const data = {
          merchantId: this.merchantId,
          items: this.buildOrderItems(),
          address: this.formatAddress(this.selectedAddress),
          phone: this.selectedAddress.phone,
          remark: this.remark,
          deliveryFee: this.deliveryFee,
          userCouponId: this.selectedCouponId
        }
        const order = await createOrder(data)
        this.$store.commit('CLEAR_CART')
        uni.showToast({ title: this.text.submitSuccess, icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: `/pages/order/detail?id=${order.id || order}` })
        }, 1000)
      } catch (error) {
        console.error(error)
      } finally {
        this.submitting = false
      }
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

.address-section,
.merchant-section,
.dish-section,
.coupon-section,
.remark-section,
.price-section {
  background: #fff;
  margin-bottom: 20rpx;
}

.address-section {
  padding: 30rpx;
  display: flex;
  align-items: center;
}

.address-card {
  flex: 1;
}

.address-user {
  margin-bottom: 10rpx;
}

.name {
  font-size: 30rpx;
  font-weight: bold;
  margin-right: 20rpx;
}

.phone {
  font-size: 26rpx;
  color: #666;
}

.address-detail {
  font-size: 26rpx;
  color: #666;
}

.address-empty {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.address-empty text {
  font-size: 28rpx;
  color: #999;
}

.merchant-section,
.remark-section,
.coupon-section,
.price-section {
  padding: 24rpx 30rpx;
}

.merchant-name {
  font-size: 30rpx;
  font-weight: bold;
}

.dish-section {
  padding: 20rpx 30rpx;
}

.dish-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.dish-item:last-child {
  border-bottom: none;
}

.dish-main {
  flex: 1;
  min-width: 0;
}

.dish-name {
  display: block;
  font-size: 28rpx;
}

.dish-summary {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a8f99;
  line-height: 1.5;
}

.dish-quantity {
  font-size: 26rpx;
  color: #999;
}

.dish-price {
  font-size: 28rpx;
  color: #ff6b00;
}

.coupon-section,
.remark-section {
  display: flex;
  align-items: center;
}

.label {
  font-size: 28rpx;
  margin-right: 20rpx;
  white-space: nowrap;
}

.coupon-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  min-width: 0;
}

.coupon-value {
  font-size: 26rpx;
  color: #666;
  text-align: right;
}

.coupon-value.selected {
  color: #ff6b00;
}

.coupon-value.disabled {
  color: #999;
}

.remark-input {
  flex: 1;
  font-size: 26rpx;
}

.price-row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  margin-bottom: 16rpx;
}

.price-row:last-child {
  margin-bottom: 0;
}

.price-row.total {
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
  font-size: 30rpx;
  font-weight: bold;
}

.discount-text,
.total-price {
  color: #ff6b00;
}

.submit-bar {
  position: fixed;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  height: 100rpx;
  z-index: 1101;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.submit-price {
  font-size: 28rpx;
}

.price {
  font-size: 36rpx;
  color: #ff6b00;
  font-weight: bold;
}

.submit-btn {
  background: #ff6b00;
  color: #fff;
  padding: 20rpx 50rpx;
  border-radius: 50rpx;
  font-size: 30rpx;
}

.submit-btn.disabled {
  opacity: 0.5;
}

.popup-card {
  padding: 30rpx;
}

.popup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 600;
}

.popup-link {
  font-size: 26rpx;
  color: #999;
}

.coupon-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.coupon-option.active .coupon-option-name,
.coupon-option.active .coupon-option-minus {
  color: #ff6b00;
}

.coupon-option-main {
  flex: 1;
  min-width: 0;
}

.coupon-option-name {
  display: block;
  font-size: 28rpx;
  color: #333;
}

.coupon-option-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.coupon-option-side {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.coupon-option-minus {
  font-size: 26rpx;
  color: #666;
}

.empty-coupon {
  padding: 40rpx 0;
  text-align: center;
  color: #999;
  font-size: 26rpx;
}
</style>

