import request, { BASE_URL } from '@/utils/request'
import { handleUnauthorized } from '@/utils/session'

export const getProfile = () => {
  return request({
    url: '/student/user/profile',
    method: 'GET'
  })
}

export const updateProfile = (data) => {
  return request({
    url: '/student/user/profile',
    method: 'PUT',
    data
  })
}

export const uploadAvatar = (filePath) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: `${BASE_URL}/student/user/avatar`,
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        let parsed
        try {
          parsed = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        } catch (error) {
          uni.showToast({ title: '头像上传失败', icon: 'none' })
          reject(error)
          return
        }

        if (res.statusCode === 200 && parsed && parsed.code === 0) {
          resolve(parsed.data)
          return
        }

        const message = (parsed && parsed.message) || '头像上传失败'
        if (res.statusCode === 401 || (parsed && parsed.code === 401)) {
          uni.showToast({ title: message || '请先登录', icon: 'none' })
          handleUnauthorized()
          reject({ message })
          return
        }

        uni.showToast({ title: message, icon: 'none' })
        reject({ message })
      },
      fail: (error) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(error)
      }
    })
  })
}

export const getAddresses = () => {
  return request({
    url: '/student/user/addresses',
    method: 'GET'
  })
}

export const addAddress = (data) => {
  return request({
    url: '/student/user/addresses',
    method: 'POST',
    data
  })
}

export const updateAddress = (id, data) => {
  return request({
    url: `/student/user/addresses/${id}`,
    method: 'PUT',
    data
  })
}

export const deleteAddress = (id) => {
  return request({
    url: `/student/user/addresses/${id}`,
    method: 'DELETE'
  })
}

export const setDefaultAddress = (id) => {
  return request({
    url: `/student/user/addresses/${id}/default`,
    method: 'PUT'
  })
}
