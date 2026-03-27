<template>
  <view class="container">
    <view class="address-section" @click="chooseAddress">
      <view v-if="selectedAddress" class="address-card">
        <view class="address-user">
          <text class="name">{{ selectedAddress.name }}</text>
          <text class="phone">{{ selectedAddress.phone }}</text>
        </view>
        <view class="address-detail">{{ selectedAddress.address }} {{ selectedAddress.building }} {{ selectedAddress.room }}</view>
      </view>
      <view v-else class="address-empty">
        <u-icon name="plus-circle" size="24" color="#ff6b00"></u-icon>
        <text>请选择收货地址</text>
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
        <text class="dish-price">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
      </view>
    </view>

    <view class="remark-section">
      <text class="label">备注</text>
      <input class="remark-input" v-model="remark" placeholder="口味、偏好等（选填）" />
    </view>

    <view class="price-section">
      <view class="price-row">
        <text>菜品总价</text>
        <text>¥{{ dishTotal }}</text>
      </view>
      <view class="price-row">
        <text>配送费</text>
        <text>¥{{ deliveryFee }}</text>
      </view>
      <view class="price-row total">
        <text>实付金额</text>
        <text class="total-price">¥{{ totalPrice }}</text>
      </view>
    </view>

    <view class="submit-bar">
      <view class="submit-price">
        <text>合计：</text>
        <text class="price">¥{{ totalPrice }}</text>
      </view>
      <view class="submit-btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">提交订单</view>
    </view>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { createOrder } from '@/api/order'
import { getAddresses } from '@/api/user'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      merchantId: '',
      merchantName: '',
      selectedAddress: null,
      remark: '',
      deliveryFee: 3,
      submitting: false
    }
  },
  computed: {
    ...mapState(['cart']),
    ...mapGetters(['cartAmount']),
    dishTotal() {
      return this.cartAmount.toFixed(2)
    },
    totalPrice() {
      return (this.cartAmount + this.deliveryFee).toFixed(2)
    },
    canSubmit() {
      return this.selectedAddress && this.cart.length > 0 && !this.submitting
    }
  },
  onLoad(options) {
    this.merchantId = options.merchantId || ''
    this.merchantName = decodeURIComponent(options.merchantName || '')
    this.loadDefaultAddress()
  },
  methods: {
    async loadDefaultAddress() {
      try {
        const list = await getAddresses()
        this.selectedAddress = list.find((item) => item.isDefault) || list[0] || null
      } catch (error) {
        console.error(error)
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
    buildOrderItems() {
      return this.cart.map((item) => ({
        dishId: item.dishId || item.id,
        type: item.type === 'combo' ? 'combo' : 'single',
        name: item.name,
        image: item.image || '',
        price: item.price,
        quantity: item.quantity,
        comboSelections: Array.isArray(item.comboSelections)
          ? item.comboSelections.map((group, groupIndex) => ({
              groupIndex: group.groupIndex !== undefined && group.groupIndex !== null ? group.groupIndex : groupIndex,
              name: group.name || '',
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
          uni.showToast({ title: '请选择收货地址', icon: 'none' })
        }
        return
      }

      this.submitting = true
      try {
        const data = {
          merchantId: this.merchantId,
          items: this.buildOrderItems(),
          address: `${this.selectedAddress.address} ${this.selectedAddress.building} ${this.selectedAddress.room}`,
          phone: this.selectedAddress.phone,
          remark: this.remark,
          deliveryFee: this.deliveryFee
        }
        const order = await createOrder(data)
        this.$store.commit('CLEAR_CART')
        uni.showToast({ title: '支付成功（模拟）', icon: 'success' })
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
.container { background: #f8f8f8; min-height: 100vh; padding-bottom: 120rpx; }

.address-section {
  background: #fff; padding: 30rpx; margin-bottom: 20rpx;
  display: flex; align-items: center;
}

.address-card { flex: 1; }
.address-user { margin-bottom: 10rpx; }
.name { font-size: 30rpx; font-weight: bold; margin-right: 20rpx; }
.phone { font-size: 26rpx; color: #666; }
.address-detail { font-size: 26rpx; color: #666; }

.address-empty { flex: 1; display: flex; align-items: center; gap: 10rpx; }
.address-empty text { font-size: 28rpx; color: #999; }

.merchant-section {
  background: #fff; padding: 24rpx 30rpx; margin-bottom: 20rpx;
}

.merchant-name { font-size: 30rpx; font-weight: bold; }

.dish-section {
  background: #fff; padding: 20rpx 30rpx; margin-bottom: 20rpx;
}

.dish-item {
  display: flex; align-items: center; gap: 20rpx;
  padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5;
}

.dish-item:last-child { border-bottom: none; }
.dish-main { flex: 1; min-width: 0; }
.dish-name { display: block; font-size: 28rpx; }
.dish-summary { display: block; margin-top: 6rpx; font-size: 22rpx; color: #8a8f99; line-height: 1.5; }
.dish-quantity { font-size: 26rpx; color: #999; }
.dish-price { font-size: 28rpx; color: #ff6b00; }

.remark-section {
  background: #fff; padding: 24rpx 30rpx; margin-bottom: 20rpx;
  display: flex; align-items: center;
}

.label { font-size: 28rpx; margin-right: 20rpx; white-space: nowrap; }
.remark-input { flex: 1; font-size: 26rpx; }

.price-section {
  background: #fff; padding: 24rpx 30rpx; margin-bottom: 20rpx;
}

.price-row {
  display: flex; justify-content: space-between; font-size: 26rpx; margin-bottom: 16rpx;
}

.price-row:last-child { margin-bottom: 0; }
.price-row.total { padding-top: 16rpx; border-top: 1rpx solid #f0f0f0; font-size: 30rpx; font-weight: bold; }
.total-price { color: #ff6b00; }

.submit-bar {
  position: fixed;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  height: 100rpx;
  z-index: 1101;
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  padding: 0 30rpx; box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.1);
}

.submit-price { font-size: 28rpx; }
.price { font-size: 36rpx; color: #ff6b00; font-weight: bold; }
.submit-btn {
  background: #ff6b00; color: #fff; padding: 20rpx 50rpx;
  border-radius: 50rpx; font-size: 30rpx;
}

.submit-btn.disabled { opacity: 0.5; }
</style>
