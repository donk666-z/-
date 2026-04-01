import request from '@/utils/request'

export const wxLogin = (code) => {
  return request({
    url: '/auth/wx-login',
    method: 'POST',
    data: { code }
  })
}

export const studentLogin = (data) => {
  return request({
    url: '/auth/student-login',
    method: 'POST',
    data
  })
}

export const studentRegister = (data) => {
  return request({
    url: '/auth/student-register',
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
