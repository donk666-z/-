import request from './request'

export function getConfigList() {
  return request.get('/config')
}

export function updateConfig(data) {
  return request.put('/config', data)
}
