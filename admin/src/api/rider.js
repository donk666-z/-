import request from './request'

export function getRiderList(params) {
  return request.get('/admin/rider/list', { params })
}

export function updateRiderStatus(id, status) {
  return request.put(`/admin/rider/${id}/status`, { status })
}
