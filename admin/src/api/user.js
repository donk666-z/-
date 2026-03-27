import request from './request'

export function getUserList(params) {
  return request.get('/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, { status })
}
