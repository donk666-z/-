import request from './request'

export function getConfigList() {
  return request.get('/admin/config/list')
}

export function updateConfig(key, data) {
  return request.put(`/admin/config/${key}`, data)
}
