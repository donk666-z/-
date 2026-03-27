import request from './request'

export function getOrderList(params) {
  return request.get('/orders', { params })
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

export function cancelOrder(id) {
  return request.put(`/orders/${id}/cancel`)
}
