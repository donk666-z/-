import request from '@/utils/request'

export const updateLocation = (data) => {
  return request({
    url: '/rider/location/update',
    method: 'POST',
    data
  })
}
