import request from '@/utils/request'

export const riderLogin = (data) => {
  return request({
    url: '/auth/rider-login',
    method: 'POST',
    data
  })
}

export const riderRegister = (data) => {
  return request({
    url: '/auth/rider-register',
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
