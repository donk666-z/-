import request from '@/utils/request'

export const wxLogin = (code) => {
  return request({
    url: '/auth/wx-login',
    method: 'POST',
    data: { code }
  })
}

export const merchantRegister = (data) => {
  return request({
    url: '/auth/merchant-register',
    method: 'POST',
    data
  })
}

export const merchantLogin = (data) => {
  return request({
    url: '/auth/merchant-login',
    method: 'POST',
    data
  })
}

export const getUserInfo = () => {
  return request({
    url: '/auth/user-info',
    method: 'GET'
  })
}
