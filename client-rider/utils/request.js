import { handleUnauthorized } from '@/utils/session'

const BASE_URL = 'http://localhost:8080/api'

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 0) {
            resolve(res.data.data)
            return
          }
          const error = {
            ...res.data,
            message: res.data.message || '请求失败'
          }
          uni.showToast({ title: error.message, icon: 'none' })
          reject(error)
          return
        }

        if (res.statusCode === 401) {
          uni.showToast({ title: '请先登录', icon: 'none' })
          handleUnauthorized()
          reject({ message: '请先登录' })
          return
        }

        if (res.statusCode === 403) {
          const message = (res.data && res.data.message) || '暂无权限访问'
          if (options.url && options.url.indexOf('/rider/') === 0 && token) {
            uni.showToast({ title: '登录身份已失效，请重新登录', icon: 'none' })
            handleUnauthorized()
            reject({ message: '登录身份已失效，请重新登录' })
            return
          }
          uni.showToast({ title: message, icon: 'none' })
          reject({ message })
          return
        }

        uni.showToast({ title: '网络错误', icon: 'none' })
        reject({ message: '网络错误' })
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default request
