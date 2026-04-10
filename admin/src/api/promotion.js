import request from './request'

export function getPromotionList(params) {
  return request.get('/admin/promotion/list', { params })
}

export function getPromotionDetail(id) {
  return request.get(`/admin/promotion/${id}`)
}

export function createPromotion(data) {
  return request.post('/admin/promotion', data)
}

export function updatePromotion(id, data) {
  return request.put(`/admin/promotion/${id}`, data)
}

export function updatePromotionStatus(id, status) {
  return request.put(`/admin/promotion/${id}/status`, { status })
}
