import request from './request'

export function adminLogin(data) {
  return request.post('/auth/admin-login', data)
}
