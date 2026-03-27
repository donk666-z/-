import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const toNumber = (value) => {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}

const normalizeComboSelections = (comboSelections) => {
  if (!Array.isArray(comboSelections)) {
    return []
  }

  return comboSelections
    .map((group, groupIndex) => {
      if (!group) {
        return null
      }

      const options = Array.isArray(group.options)
        ? group.options
            .map((option) => {
              if (!option || option.dishId === undefined || option.dishId === null) {
                return null
              }
              return {
                dishId: Number(option.dishId),
                dishName: option.dishName || option.name || '',
                dishImage: option.dishImage || option.image || '',
                quantity: Math.max(1, toNumber(option.quantity) || 1),
                extraPrice: toNumber(option.extraPrice)
              }
            })
            .filter(Boolean)
            .sort((left, right) => left.dishId - right.dishId)
        : []

      return {
        groupIndex: group.groupIndex !== undefined && group.groupIndex !== null ? Number(group.groupIndex) : groupIndex,
        name: group.name || '',
        options
      }
    })
    .filter(Boolean)
    .sort((left, right) => left.groupIndex - right.groupIndex)
}

const buildSelectionSignature = (comboSelections) => {
  return JSON.stringify(
    normalizeComboSelections(comboSelections).map((group) => ({
      groupIndex: group.groupIndex,
      options: group.options.map((option) => ({
        dishId: option.dishId,
        quantity: option.quantity
      }))
    }))
  )
}

const buildComboSummary = (comboSelections) => {
  return normalizeComboSelections(comboSelections)
    .filter((group) => Array.isArray(group.options) && group.options.length > 0)
    .map((group) => {
      const optionNames = group.options.map((option) => option.dishName).filter(Boolean).join('、')
      return `${group.name || '已选'}: ${optionNames}`
    })
    .join('；')
}

const buildCartKey = ({ dishId, type, comboSelections, cartKey }) => {
  if (cartKey) {
    return cartKey
  }
  if (type === 'combo') {
    return `combo-${dishId}-${buildSelectionSignature(comboSelections)}`
  }
  return `single-${dishId}`
}

const normalizeCartItem = (item) => {
  if (!item) {
    return null
  }

  const dishId = item.dishId !== undefined && item.dishId !== null ? Number(item.dishId) : Number(item.id)
  if (!Number.isFinite(dishId)) {
    return null
  }

  const type = item.type === 'combo' ? 'combo' : 'single'
  const comboSelections = normalizeComboSelections(item.comboSelections)
  const cartKey = buildCartKey({ dishId, type, comboSelections, cartKey: item.cartKey })

  return {
    ...item,
    id: dishId,
    dishId,
    type,
    cartKey,
    price: toNumber(item.price),
    quantity: toNumber(item.quantity),
    comboSelections,
    comboSummary: item.comboSummary || buildComboSummary(comboSelections)
  }
}

const normalizeCart = (cart) => {
  if (!Array.isArray(cart)) {
    return []
  }
  return cart.map(normalizeCartItem).filter(Boolean)
}

const savedCart = normalizeCart(uni.getStorageSync('cart'))
const savedMerchantId = uni.getStorageSync('cartMerchantId') || null

let saveTimer = null
const saveCart = (cart) => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    uni.setStorageSync('cart', cart)
  }, 300)
}

export default new Vuex.Store({
  state: {
    userInfo: null,
    token: uni.getStorageSync('token') || '',
    cart: savedCart,
    cartMerchantId: savedMerchantId,
    orderTab: null
  },
  mutations: {
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
    },
    SET_TOKEN(state, token) {
      state.token = token
      uni.setStorageSync('token', token)
    },
    SET_CART_MERCHANT(state, merchantId) {
      state.cartMerchantId = merchantId
      uni.setStorageSync('cartMerchantId', merchantId)
    },
    ADD_TO_CART(state, item) {
      const normalizedItem = normalizeCartItem(item)
      if (!normalizedItem) {
        return
      }

      const exist = state.cart.find((current) => current.cartKey === normalizedItem.cartKey)
      if (normalizedItem.quantity <= 0) {
        state.cart = state.cart.filter((current) => current.cartKey !== normalizedItem.cartKey)
      } else if (exist) {
        exist.quantity = normalizedItem.quantity
        exist.name = normalizedItem.name
        exist.price = normalizedItem.price
        exist.image = normalizedItem.image || exist.image
        exist.comboSelections = normalizedItem.comboSelections
        exist.comboSummary = normalizedItem.comboSummary
        exist.type = normalizedItem.type
      } else {
        state.cart.push(normalizedItem)
      }
      saveCart(state.cart)
    },
    UPDATE_CART_ITEM(state, { cartKey, quantity }) {
      const item = state.cart.find((current) => current.cartKey === cartKey)
      if (item) {
        item.quantity = toNumber(quantity)
        if (item.quantity <= 0) {
          state.cart = state.cart.filter((current) => current.cartKey !== cartKey)
        }
      }
      saveCart(state.cart)
    },
    CLEAR_CART(state) {
      state.cart = []
      state.cartMerchantId = null
      uni.removeStorageSync('cart')
      uni.removeStorageSync('cartMerchantId')
    },
    SET_ORDER_TAB(state, tab) {
      state.orderTab = tab
    },
    LOGOUT(state) {
      state.userInfo = null
      state.token = ''
      state.cart = []
      state.cartMerchantId = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('cart')
      uni.removeStorageSync('cartMerchantId')
    }
  },
  actions: {
    login({ commit }, { token, userInfo }) {
      commit('SET_TOKEN', token)
      commit('SET_USER_INFO', userInfo)
    },
    logout({ commit }) {
      commit('LOGOUT')
    }
  },
  getters: {
    isLoggedIn(state) {
      return !!state.token
    },
    cartTotal(state) {
      return state.cart.reduce((sum, item) => sum + toNumber(item.quantity), 0)
    },
    cartAmount(state) {
      return state.cart.reduce((sum, item) => sum + toNumber(item.price) * toNumber(item.quantity), 0)
    }
  }
})
