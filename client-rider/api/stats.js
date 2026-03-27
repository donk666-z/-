import request from '@/utils/request'

export const getStats = () => {
  return request({
    url: '/rider/stats',
    method: 'GET'
  })
}
