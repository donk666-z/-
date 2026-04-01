import request from './request'

export function getOrderList(params) {
  return request.get('/admin/order/list', { params })
}

export function getOrderDetail(id) {
  return request.get(`/admin/order/${id}`)
}

export function cancelOrder(id) {
  return request.post(`/admin/order/${id}/cancel`)
}
