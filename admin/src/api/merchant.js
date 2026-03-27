import request from './request'

export function getMerchantList(params) {
  return request.get('/merchants', { params })
}

export function updateMerchantStatus(id, status) {
  return request.put(`/merchants/${id}/status`, { status })
}
