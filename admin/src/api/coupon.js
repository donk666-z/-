import request from './request'

export function getCouponList(params) {
  return request.get('/admin/coupon/list', { params })
}

export function createCoupon(data) {
  return request.post('/admin/coupon', data)
}

export function updateCoupon(id, data) {
  return request.put(`/admin/coupon/${id}`, data)
}

export function deleteCoupon(id) {
  return request.delete(`/admin/coupon/${id}`)
}
