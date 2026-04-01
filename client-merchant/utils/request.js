import { API_BASE_URL } from '@/config/api'
import { handleUnauthorized } from '@/utils/session'

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    console.log('Request token:', token ? token.substring(0, 20) + '...' : 'null')

    uni.request({
      url: API_BASE_URL + options.url,
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
            uni.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '请先登录', icon: 'none' })
          handleUnauthorized()
          reject({ message: '请先登录' })
        } else if (res.statusCode === 403) {
          const message = (res.data && res.data.message) || '暂无权限访问'
          const shouldKick = !!token && (
            message.indexOf('禁用') >= 0 ||
            message.indexOf('失效') >= 0 ||
            message.indexOf('重新登录') >= 0
          )
          if (shouldKick) {
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
