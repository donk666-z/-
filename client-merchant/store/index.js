import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    userInfo: null,
    token: '',
    merchantInfo: null,
    pendingCount: 0
  },
  mutations: {
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
    },
    SET_TOKEN(state, token) {
      state.token = token
      uni.setStorageSync('token', token)
    },
    SET_MERCHANT_INFO(state, merchantInfo) {
      state.merchantInfo = merchantInfo
    },
    SET_PENDING_COUNT(state, count) {
      state.pendingCount = count
    },
    LOGOUT(state) {
      state.userInfo = null
      state.token = ''
      state.merchantInfo = null
      state.pendingCount = 0
      uni.removeStorageSync('token')
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
    }
  }
})
