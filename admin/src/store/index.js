import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '../utils/auth'
import { adminLogin } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: {}
  }),
  actions: {
    async login(loginForm) {
      const res = await adminLogin(loginForm)
      const { token, userInfo } = res.data || {}
      this.token = token
      this.userInfo = userInfo || {}
      setToken(token)
      return res
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      removeToken()
    }
  }
})
