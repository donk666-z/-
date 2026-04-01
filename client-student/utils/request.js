import { handleUnauthorized } from '@/utils/session'
export const BASE_URL = 'http://localhost:8080/api'

const shouldKickByMessage = (message) => {
  if (!message) return false
  const text = String(message)
  return text.indexOf('禁用') >= 0 || text.indexOf('失效') >= 0 || text.indexOf('重新登录') >= 0
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 0) {
            resolve(res.data.data)
          } else {
            const message = res.data.message || '请求失败'
            if (res.data.code === 401 && token) {
              uni.showToast({ title: message || '请先登录', icon: 'none' })
              handleUnauthorized()
              reject({ ...res.data, message })
              return
            }
            if (res.data.code === 403 && token && shouldKickByMessage(message)) {
              uni.showToast({ title: message, icon: 'none' })
              handleUnauthorized()
              reject({ ...res.data, message })
              return
            }
            uni.showToast({ title: message, icon: 'none' })
            reject({ ...res.data, message })
          }
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '请先登录', icon: 'none' })
          handleUnauthorized()
          reject({ message: '请先登录' })
        } else if (res.statusCode === 403) {
          const message = (res.data && res.data.message) || '暂无权限访问'
          if (token && shouldKickByMessage(message)) {
            uni.showToast({ title: message, icon: 'none' })
            handleUnauthorized()
            reject({ message })
            return
          }
          uni.showToast({ title: message, icon: 'none' })
          reject({ message })
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
