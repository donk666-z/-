import request from './request'

export function getMerchantList(params) {
  return request.get('/admin/merchant/list', { params })
}

export function updateMerchantStatus(id, status) {
  return request.put(`/admin/merchant/${id}/status`, { status })
}
