import request from '@/utils/request'

export const createOrder = (data) => {
  return request({
    url: '/student/order/create',
    method: 'POST',
    data
  })
}

export const getOrderList = (params) => {
  return request({
    url: '/student/order/list',
    method: 'GET',
    data: params
  })
}

export const getOrderDetail = (id) => {
  return request({
    url: `/student/order/${id}`,
    method: 'GET'
  })
}

export const getOrderRoute = (id) => {
  return request({
    url: `/student/order/${id}/route`,
    method: 'GET'
  })
}

export const cancelOrder = (id) => {
  return request({
    url: `/student/order/${id}/cancel`,
    method: 'POST'
  })
}

export const confirmOrder = (id) => {
  return request({
    url: `/student/order/${id}/confirm`,
    method: 'POST'
  })
}
