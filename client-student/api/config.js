import request from '@/utils/request'

export const getDeliveryFee = () => {
  return request({
    url: '/student/config/delivery-fee',
    method: 'GET'
  })
}
