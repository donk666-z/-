import request from '@/utils/request'

export const wxLogin = (code) => {
  return request({
    url: '/auth/wx-login',
    method: 'POST',
    data: { code }
  })
}

export const getUserInfo = () => {
  return request({
    url: '/auth/user-info',
    method: 'GET'
  })
}
