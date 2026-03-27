import request from '@/utils/request'

export const getDashboard = () => {
  return request({
    url: '/merchant/stats/dashboard',
    method: 'GET'
  })
}
