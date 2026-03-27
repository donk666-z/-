import Vue from 'vue'
import Vuex from 'vuex'
import { getStoredUserInfo } from '@/utils/session'

Vue.use(Vuex)

const rawSavedToken = uni.getStorageSync('token') || ''
const rawSavedUserInfo = getStoredUserInfo()
const isSavedRiderToken = !!rawSavedToken && !!rawSavedUserInfo && rawSavedUserInfo.role === 'rider'
const savedToken = isSavedRiderToken ? rawSavedToken : ''
const savedUserInfo = isSavedRiderToken ? rawSavedUserInfo : null
const savedRiderStatus = isSavedRiderToken ? (uni.getStorageSync('riderStatus') || 'offline') : 'offline'

if (!isSavedRiderToken && rawSavedToken) {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('riderStatus')
}

export default new Vuex.Store({
  state: {
    userInfo: savedUserInfo,
    token: savedToken,
    riderStatus: savedRiderStatus,
    currentTask: null
  },
  mutations: {
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo || null
      if (userInfo) {
        uni.setStorageSync('userInfo', userInfo)
      } else {
        uni.removeStorageSync('userInfo')
      }
    },
    SET_TOKEN(state, token) {
      state.token = token || ''
      if (token) {
        uni.setStorageSync('token', token)
      } else {
        uni.removeStorageSync('token')
      }
    },
    SET_RIDER_STATUS(state, status) {
      state.riderStatus = status || 'offline'
      uni.setStorageSync('riderStatus', state.riderStatus)
    },
    SET_CURRENT_TASK(state, task) {
      state.currentTask = task || null
    },
    LOGOUT(state) {
      state.userInfo = null
      state.token = ''
      state.riderStatus = 'offline'
      state.currentTask = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('riderStatus')
    }
  },
  actions: {
    login({ commit }, { token, userInfo, riderStatus }) {
      commit('SET_TOKEN', token)
      commit('SET_USER_INFO', userInfo)
      commit('SET_RIDER_STATUS', riderStatus || 'offline')
    },
    logout({ commit }) {
      commit('LOGOUT')
    }
  },
  getters: {
    isLoggedIn(state) {
      return !!state.token && !!state.userInfo && state.userInfo.role === 'rider'
    },
    isOnline(state) {
      return state.riderStatus === 'online' || state.riderStatus === 'delivering'
    },
    hasActiveTask(state) {
      return state.currentTask !== null
    }
  }
})
