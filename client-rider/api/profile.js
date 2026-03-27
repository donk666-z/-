import request from '@/utils/request'

export const getRiderProfile = () => {
  return request({
    url: '/rider/profile',
    method: 'GET'
  })
}

export const updateRiderStatus = (status) => {
  return request({
    url: '/rider/profile/status',
    method: 'PUT',
    data: { status }
  })
}
