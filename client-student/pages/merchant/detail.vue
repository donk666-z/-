<template>
  <view class="page">
    <view class="header-card">
      <text class="merchant-name">{{ merchant.name || '商家详情' }}</text>
      <text class="merchant-meta">评分 {{ reviewScore }} · 月售 {{ merchant.monthSales || 0 }} · 配送费 3元</text>
      <text class="merchant-desc">{{ merchant.description || '欢迎选购本店商品' }}</text>
    </view>

    <u-tabs :list="tabs" :current="currentTab" :is-scroll="false" @change="onTabChange"></u-tabs>

    <view v-if="currentTab === 0" class="panel">
      <view v-for="category in displayCategories" :key="category.id" class="category-block">
        <text class="category-name">{{ category.name }}</text>
        <view v-for="dish in category.dishes" :key="dish.id" class="dish-card">
          <view class="dish-main">
            <view class="dish-title">
              <text class="dish-name">{{ dish.name }}</text>
              <text v-if="dish.type === 'combo'" class="dish-tag">套餐</text>
            </view>
            <text class="dish-desc">{{ dish.description || '暂无描述' }}</text>
            <text v-if="dish.type === 'combo' && dish.comboSummary" class="dish-summary">{{ dish.comboSummary }}</text>
            <view class="dish-bottom">
              <text class="dish-price">{{ formatPrice(dish.price) }}元{{ dish.type === 'combo' ? '起' : '' }}</text>
              <text class="dish-stock" :class="{ soldout: dish.stock <= 0 }">{{ dish.stock <= 0 ? '已售罄' : `库存 ${dish.stock}` }}</text>
            </view>
          </view>
          <view class="dish-action">
            <u-number-box
              v-if="dish.type !== 'combo'"
              v-model="dish.quantity"
              :min="0"
              :max="dish.stock > 0 ? dish.stock : 0"
              :disabled="dish.stock <= 0"
              @change="onSingleQuantityChange(dish, $event)"
            ></u-number-box>
            <view v-else class="combo-btn" :class="{ disabled: dish.stock <= 0 }" @click="openComboPopup(dish)">
              {{ dish.comboCartCount > 0 ? `已选 ${dish.comboCartCount}` : '选套餐' }}
            </view>
          </view>
        </view>
      </view>
      <view v-if="displayCategories.length === 0" class="empty-text">商家暂时还没有上架菜品</view>
    </view>

    <view v-else-if="currentTab === 1" class="panel">
      <view v-if="reviews.length === 0" class="empty-text">这家店铺暂时还没有评价</view>
      <view v-else class="review-list">
        <view v-for="review in reviews" :key="review.id" class="review-card">
          <view class="review-head">
            <text>{{ review.nickname || '匿名用户' }}</text>
            <u-rate :value="Number(review.foodRating || 0)" :count="5" size="12" active-color="#ff6b00" disabled></u-rate>
          </view>
          <text class="review-content">{{ review.content || '用户未填写评价内容' }}</text>
        </view>
      </view>
    </view>

    <view v-else class="panel">
      <view class="info-card">
        <view class="info-row"><text>营业状态</text><text>{{ merchant.status === 'open' ? '营业中' : '休息中' }}</text></view>
        <view class="info-row"><text>营业时间</text><text>{{ merchant.businessHours || '商家暂未填写' }}</text></view>
        <view class="info-row"><text>店铺地址</text><text>{{ merchant.address || '商家暂未填写' }}</text></view>
        <view class="info-row"><text>联系电话</text><text>{{ merchant.phone || '商家暂未填写' }}</text></view>
      </view>
      <button v-if="merchant.phone" class="contact-btn" @click="callMerchant">联系商家</button>
    </view>

    <view v-if="cartCount > 0 && currentTab === 0" class="cart-bar">
      <view class="cart-left" @click="showCart = true">
        <text class="cart-count">{{ cartCount }}</text>
        <text class="cart-total">{{ cartTotal }}元</text>
      </view>
      <view class="cart-submit" @click="goToCheckout">去结算</view>
    </view>

    <u-popup :show="showCart" mode="bottom" @close="showCart = false" round="20">
      <view class="popup-card">
        <view class="popup-head">
          <text class="popup-title">已选商品</text>
          <text class="popup-link" @click="clearCart">清空</text>
        </view>
        <view v-for="item in cartItems" :key="item.cartKey" class="cart-item">
          <view class="cart-item-main">
            <text class="item-name">{{ item.name }}</text>
            <text v-if="item.type === 'combo' && item.comboSummary" class="item-summary">{{ item.comboSummary }}</text>
          </view>
          <text class="item-price">{{ formatAmount(item.price, item.quantity) }}元</text>
          <u-number-box v-model="item.quantity" :min="0" @change="onCartItemChange(item, $event)"></u-number-box>
        </view>
      </view>
    </u-popup>

    <u-popup :show="showComboPopup" mode="bottom" @close="closeComboPopup" round="20">
      <view class="popup-card" v-if="activeComboDish">
        <view class="popup-head">
          <text class="popup-title">{{ activeComboDish.name }}</text>
          <text class="popup-link" @click="closeComboPopup">关闭</text>
        </view>
        <view v-for="(group, groupIndex) in activeComboSelections" :key="groupIndex" class="combo-group">
          <text class="combo-group-title">{{ group.name }}，至少 {{ group.minSelect }} 项，最多 {{ group.maxSelect }} 项</text>
          <view
            v-for="option in group.allOptions"
            :key="option.dishId"
            class="combo-option"
            :class="{ active: isComboOptionSelected(groupIndex, option.dishId), disabled: !option.available }"
            @click="toggleComboOption(groupIndex, option)"
          >
            <view>
              <text class="combo-option-name">{{ option.dishName }}</text>
              <text class="combo-option-meta">x{{ option.quantity }}{{ option.extraPrice > 0 ? `  +${formatPrice(option.extraPrice)}元` : '' }}</text>
            </view>
            <text class="combo-option-stock">{{ option.available ? `库存 ${option.dishStock}` : '暂不可选' }}</text>
          </view>
        </view>
        <view class="combo-footer">
          <text class="combo-price">¥{{ getActiveComboPrice() }}</text>
          <u-number-box v-model="activeComboQuantity" :min="1" :max="99"></u-number-box>
          <view class="cart-submit" @click="addActiveComboToCart">加入购物车</view>
        </view>
      </view>
    </u-popup>

    <StudentTabBarOverlay active="home" />
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getMerchantDetail, getDishList } from '@/api/merchant'
import { getMerchantReviews } from '@/api/review'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

const normalizeType = (type) => (type === 'combo' ? 'combo' : 'single')

const normalizeComboSelections = (comboSelections) => {
  if (!Array.isArray(comboSelections)) return []
  return comboSelections
    .map((group, groupIndex) => ({
      groupIndex: group.groupIndex !== undefined && group.groupIndex !== null ? Number(group.groupIndex) : groupIndex,
      name: group.name || '',
      options: Array.isArray(group.options)
        ? group.options
            .map((option) => {
              if (!option || option.dishId === undefined || option.dishId === null) return null
              return {
                dishId: Number(option.dishId),
                dishName: option.dishName || '',
                dishImage: option.dishImage || '',
                quantity: Number(option.quantity) || 1,
                extraPrice: Number(option.extraPrice) || 0
              }
            })
            .filter(Boolean)
            .sort((left, right) => left.dishId - right.dishId)
        : []
    }))
    .sort((left, right) => left.groupIndex - right.groupIndex)
}

const buildComboSignature = (comboSelections) =>
  JSON.stringify(
    normalizeComboSelections(comboSelections).map((group) => ({
      groupIndex: group.groupIndex,
      options: group.options.map((option) => ({
        dishId: option.dishId,
        quantity: option.quantity
      }))
    }))
  )

const buildComboSummary = (comboSelections) =>
  normalizeComboSelections(comboSelections)
    .filter((group) => group.options.length > 0)
    .map((group) => `${group.name || '已选'}: ${group.options.map((option) => option.dishName).join('、')}`)
    .join('；')

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      merchantId: '',
      merchant: {},
      categories: [],
      reviews: [],
      showCart: false,
      showComboPopup: false,
      activeComboDish: null,
      activeComboSelections: [],
      activeComboQuantity: 1,
      currentTab: 0,
      tabs: [{ name: '点菜' }, { name: '评价' }, { name: '商家' }]
    }
  },
  computed: {
    ...mapState(['cart', 'cartMerchantId']),
    safeCart() {
      return Array.isArray(this.cart)
        ? this.cart
            .map((item) => {
              if (!item) return null
              return {
                ...item,
                type: normalizeType(item.type),
                dishId: Number(item.dishId || item.id),
                price: this.toNumber(item.price),
                quantity: this.toNumber(item.quantity),
                comboSelections: normalizeComboSelections(item.comboSelections),
                comboSummary: item.comboSummary || buildComboSummary(item.comboSelections)
              }
            })
            .filter(Boolean)
        : []
    },
    displayCategories() {
      return (Array.isArray(this.categories) ? this.categories : []).map((category) => ({
        ...category,
        dishes: (Array.isArray(category.dishes) ? category.dishes : []).map((dish) => {
          const singleItem = this.safeCart.find((item) => item.type !== 'combo' && item.dishId === dish.id)
          const comboCartCount = this.safeCart
            .filter((item) => item.type === 'combo' && item.dishId === dish.id)
            .reduce((sum, item) => sum + this.toNumber(item.quantity), 0)
          return {
            ...dish,
            type: normalizeType(dish.type),
            price: this.toNumber(dish.price),
            stock: Math.max(0, this.toNumber(dish.stock)),
            quantity: singleItem ? this.toNumber(singleItem.quantity) : 0,
            comboCartCount,
            comboSummary: this.getComboDishSummary(dish)
          }
        })
      }))
    },
    cartItems() {
      return this.safeCart.filter((item) => item.quantity > 0)
    },
    cartCount() {
      return this.safeCart.reduce((sum, item) => sum + this.toNumber(item.quantity), 0)
    },
    cartTotal() {
      return this.safeCart.reduce((sum, item) => sum + this.toNumber(item.price) * this.toNumber(item.quantity), 0).toFixed(2)
    },
    reviewScore() {
      if (this.merchant && this.merchant.rating !== undefined && this.merchant.rating !== null && this.merchant.rating !== '') {
        return this.toNumber(this.merchant.rating).toFixed(1)
      }
      if (!this.reviews.length) return '-'
      const total = this.reviews.reduce((sum, item) => sum + this.toNumber(item.foodRating), 0)
      return (total / this.reviews.length).toFixed(1)
    }
  },
  onLoad(options) {
    this.merchantId = options.id
    if (this.cartMerchantId && this.cartMerchantId !== this.merchantId && this.safeCart.length > 0) {
      uni.showModal({
        title: '提示',
        content: '切换店铺将清空购物车，是否继续？',
        success: (res) => {
          if (res.confirm) {
            this.$store.commit('CLEAR_CART')
            this.$store.commit('SET_CART_MERCHANT', this.merchantId)
            this.loadData()
          } else {
            uni.navigateBack()
          }
        }
      })
      return
    }
    this.$store.commit('SET_CART_MERCHANT', this.merchantId)
    this.loadData()
  },
  methods: {
    toNumber(value) {
      const numberValue = Number(value)
      return Number.isFinite(numberValue) ? numberValue : 0
    },
    formatPrice(value) {
      const price = this.toNumber(value)
      return Number.isInteger(price) ? String(price) : price.toFixed(2)
    },
    formatAmount(price, quantity) {
      return (this.toNumber(price) * this.toNumber(quantity)).toFixed(2)
    },
    buildComboCartKey(dishId, comboSelections) {
      return `combo-${dishId}-${buildComboSignature(comboSelections)}`
    },
    getComboDishSummary(dish) {
      if (!dish || !dish.comboConfig || !Array.isArray(dish.comboConfig.groups)) return ''
      return dish.comboConfig.groups
        .map((group) => {
          const availableOptions = Array.isArray(group.options) ? group.options.filter((option) => option && option.available) : []
          return availableOptions.length ? `${group.name || '分组'}可选 ${availableOptions.map((option) => option.dishName).join('、')}` : ''
        })
        .filter(Boolean)
        .join('；')
    },
    async loadData() {
      await Promise.all([this.loadMerchantDetail(), this.loadDishList(), this.loadReviews()])
    },
    async loadMerchantDetail() {
      try {
        this.merchant = (await getMerchantDetail(this.merchantId)) || {}
      } catch (error) {
        console.error('加载商家详情失败', error)
      }
    },
    async loadDishList() {
      try {
        const response = await getDishList(this.merchantId)
        this.categories = (Array.isArray(response) ? response : []).map((category) => ({
          ...category,
          dishes: (Array.isArray(category.dishes) ? category.dishes : []).map((dish) => ({
            ...dish,
            type: normalizeType(dish.type),
            price: this.toNumber(dish.price),
            stock: Math.max(0, this.toNumber(dish.stock))
          }))
        }))
      } catch (error) {
        console.error('加载菜品失败', error)
        this.categories = []
      }
    },
    async loadReviews() {
      try {
        const response = await getMerchantReviews(this.merchantId)
        this.reviews = Array.isArray(response) ? response : []
      } catch (error) {
        console.error('加载评价失败', error)
        this.reviews = []
      }
    },
    onTabChange(item) {
      this.currentTab = item.index
    },
    onSingleQuantityChange(dish, event) {
      const quantity = this.toNumber(event && event.value !== undefined ? event.value : dish.quantity)
      dish.quantity = quantity
      this.$store.commit('ADD_TO_CART', {
        type: 'single',
        dishId: dish.id,
        name: dish.name,
        price: this.toNumber(dish.price),
        image: dish.image,
        quantity
      })
    },
    openComboPopup(dish) {
      if (!dish || dish.stock <= 0) return
      this.activeComboDish = JSON.parse(JSON.stringify(dish))
      this.activeComboQuantity = 1
      this.activeComboSelections = ((dish.comboConfig && dish.comboConfig.groups) || []).map((group, groupIndex) => ({
        groupIndex,
        name: group.name || '',
        minSelect: Number(group.minSelect || 0),
        maxSelect: Number(group.maxSelect || 0),
        options: [],
        allOptions: Array.isArray(group.options)
          ? group.options.map((option) => ({
              dishId: Number(option.dishId),
              dishName: option.dishName || '',
              dishImage: option.dishImage || '',
              quantity: Number(option.quantity) || 1,
              extraPrice: Number(option.extraPrice) || 0,
              available: !!option.available,
              dishStock: Number(option.dishStock || 0)
            }))
          : []
      }))
      this.showComboPopup = true
    },
    closeComboPopup() {
      this.showComboPopup = false
      this.activeComboDish = null
      this.activeComboSelections = []
      this.activeComboQuantity = 1
    },
    isComboOptionSelected(groupIndex, dishId) {
      const group = this.activeComboSelections[groupIndex]
      return !!group && group.options.some((option) => option.dishId === dishId)
    },
    toggleComboOption(groupIndex, option) {
      const group = this.activeComboSelections[groupIndex]
      if (!group || !option || !option.available) {
        if (option && !option.available) uni.showToast({ title: '该选项暂不可选', icon: 'none' })
        return
      }
      const existingIndex = group.options.findIndex((current) => current.dishId === option.dishId)
      if (group.maxSelect === 1) {
        group.options = [this.pickComboOption(option)]
        return
      }
      if (existingIndex >= 0) {
        group.options.splice(existingIndex, 1)
        return
      }
      if (group.maxSelect > 0 && group.options.length >= group.maxSelect) {
        uni.showToast({ title: `最多选择 ${group.maxSelect} 项`, icon: 'none' })
        return
      }
      group.options.push(this.pickComboOption(option))
      group.options.sort((left, right) => left.dishId - right.dishId)
    },
    pickComboOption(option) {
      return {
        dishId: option.dishId,
        dishName: option.dishName,
        dishImage: option.dishImage,
        quantity: option.quantity,
        extraPrice: option.extraPrice
      }
    },
    getActiveComboPrice() {
      if (!this.activeComboDish) return '0.00'
      const extra = this.activeComboSelections.reduce((sum, group) => {
        return sum + group.options.reduce((groupSum, option) => groupSum + this.toNumber(option.extraPrice), 0)
      }, 0)
      return (this.toNumber(this.activeComboDish.price) + extra).toFixed(2)
    },
    validateActiveComboSelections() {
      for (const group of this.activeComboSelections) {
        const count = group.options.length
        if (count < group.minSelect) {
          uni.showToast({ title: `${group.name} 至少选择 ${group.minSelect} 项`, icon: 'none' })
          return false
        }
        if (group.maxSelect > 0 && count > group.maxSelect) {
          uni.showToast({ title: `${group.name} 最多选择 ${group.maxSelect} 项`, icon: 'none' })
          return false
        }
      }
      return true
    },
    addActiveComboToCart() {
      if (!this.activeComboDish || !this.validateActiveComboSelections()) return
      const comboSelections = this.activeComboSelections.map((group) => ({
        groupIndex: group.groupIndex,
        name: group.name,
        options: group.options.map((option) => ({ ...option }))
      }))
      const cartKey = this.buildComboCartKey(this.activeComboDish.id, comboSelections)
      const existingItem = this.safeCart.find((item) => item.cartKey === cartKey)
      const nextQuantity = (existingItem ? this.toNumber(existingItem.quantity) : 0) + this.activeComboQuantity

      this.$store.commit('ADD_TO_CART', {
        cartKey,
        type: 'combo',
        dishId: this.activeComboDish.id,
        name: this.activeComboDish.name,
        price: Number(this.getActiveComboPrice()),
        image: this.activeComboDish.image,
        quantity: nextQuantity,
        comboSelections,
        comboSummary: buildComboSummary(comboSelections)
      })
      uni.showToast({ title: '已加入购物车', icon: 'none' })
      this.closeComboPopup()
    },
    onCartItemChange(item, event) {
      const quantity = this.toNumber(event && event.value !== undefined ? event.value : item.quantity)
      item.quantity = quantity
      this.$store.commit('ADD_TO_CART', { ...item, quantity })
      if (item.type !== 'combo') {
        this.categories.forEach((category) => {
          ;(category.dishes || []).forEach((dish) => {
            if (dish.id === item.dishId) {
              dish.quantity = quantity
            }
          })
        })
      }
      if (this.cartCount === 0) {
        this.showCart = false
      }
    },
    clearCart() {
      this.$store.commit('CLEAR_CART')
      this.$store.commit('SET_CART_MERCHANT', this.merchantId)
      this.categories.forEach((category) => {
        ;(category.dishes || []).forEach((dish) => {
          dish.quantity = 0
        })
      })
      this.showCart = false
    },
    goToCheckout() {
      uni.navigateTo({
        url: `/pages/order/confirm?merchantId=${this.merchantId}&merchantName=${encodeURIComponent(this.merchant.name || '')}`
      })
    },
    callMerchant() {
      if (!this.merchant.phone) {
        uni.showToast({ title: '商家暂未填写电话', icon: 'none' })
        return
      }
      uni.makePhoneCall({ phoneNumber: this.merchant.phone })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 20rpx 20rpx 140rpx;
}

.header-card,
.info-card,
.popup-card,
.review-card,
.dish-card {
  background: #fff;
  border-radius: 20rpx;
}

.header-card,
.info-card,
.popup-card,
.review-card {
  padding: 24rpx;
}

.header-card {
  margin-bottom: 20rpx;
  background: linear-gradient(135deg, #fff5ea 0%, #ffffff 100%);
}

.merchant-name {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #1f2937;
}

.merchant-meta,
.merchant-desc,
.dish-desc,
.dish-summary,
.item-summary,
.combo-option-meta,
.review-content {
  display: block;
  line-height: 1.6;
}

.merchant-meta,
.merchant-desc,
.dish-desc,
.dish-summary,
.item-summary,
.combo-option-meta,
.combo-option-stock,
.dish-stock,
.review-content {
  font-size: 24rpx;
  color: #7b8495;
}

.panel {
  padding-top: 20rpx;
}

.category-block {
  margin-bottom: 24rpx;
}

.category-name {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.dish-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 22rpx;
  margin-bottom: 16rpx;
}

.dish-main {
  flex: 1;
  min-width: 0;
}

.dish-title,
.dish-bottom,
.popup-head,
.review-head,
.info-row,
.combo-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dish-title {
  gap: 12rpx;
}

.dish-name,
.item-name,
.popup-title,
.combo-group-title,
.combo-option-name {
  font-weight: 700;
  color: #1f2937;
}

.dish-name,
.popup-title {
  font-size: 30rpx;
}

.dish-tag,
.combo-btn,
.cart-submit,
.contact-btn {
  border-radius: 999rpx;
}

.dish-tag {
  padding: 4rpx 12rpx;
  background: #fff1e6;
  color: #ff7a1a;
  font-size: 20rpx;
}

.dish-price,
.combo-price,
.cart-total,
.item-price {
  color: #ff6b00;
  font-weight: 700;
}

.dish-price,
.combo-price {
  font-size: 32rpx;
}

.dish-stock.soldout {
  color: #b9bec7;
}

.combo-btn,
.cart-submit {
  min-width: 140rpx;
  height: 64rpx;
  background: #ff6b00;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}

.combo-btn.disabled {
  background: #d7dce3;
}

.review-list,
.cart-item {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.review-card + .review-card {
  margin-top: 16rpx;
}

.info-card {
  margin-bottom: 20rpx;
}

.info-row {
  gap: 20rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f1f3f6;
}

.info-row:last-child {
  border-bottom: none;
}

.contact-btn {
  height: 88rpx;
  line-height: 88rpx;
  background: #ff6b00;
  color: #fff;
  border: none;
}

.contact-btn::after {
  border: none;
}

.cart-bar {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  z-index: 1101;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 24rpx;
  border-radius: 24rpx;
  background: #22262e;
}

.cart-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.cart-count {
  min-width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #ff6b00;
  color: #fff;
  text-align: center;
  line-height: 44rpx;
  font-size: 24rpx;
}

.cart-total {
  font-size: 34rpx;
  color: #fff;
}

.popup-head {
  margin-bottom: 18rpx;
}

.popup-link {
  font-size: 24rpx;
  color: #8a8f99;
}

.cart-item {
  flex-direction: row;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f1f3f6;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item-main {
  flex: 1;
  min-width: 0;
}

.combo-group {
  margin-top: 22rpx;
}

.combo-group-title {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
}

.combo-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 18rpx;
  margin-bottom: 12rpx;
  border-radius: 18rpx;
  background: #f7f8fb;
  border: 2rpx solid transparent;
}

.combo-option.active {
  background: #fff5ee;
  border-color: #ffb27b;
}

.combo-option.disabled {
  opacity: 0.5;
}

.empty-text {
  padding: 80rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #a0a7b4;
}
</style>
