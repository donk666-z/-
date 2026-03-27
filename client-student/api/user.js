import request from '@/utils/request'

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

