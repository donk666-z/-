import request from './request'

export function getCouponList(params) {
  return request.get('/coupons', { params })
}

export function createCoupon(data) {
  return request.post('/coupons', data)
}

export function updateCoupon(id, data) {
  return request.put(`/coupons/${id}`, data)
}

export function deleteCoupon(id) {
  return request.delete(`/coupons/${id}`)
}
