import request from '@/utils/request'

export const getMerchantList = (params) => {
  return request({
    url: '/student/merchant/list',
    method: 'GET',
    data: params
  })
}

export const getMerchantDetail = (id) => {
  return request({
    url: `/student/merchant/${id}`,
    method: 'GET'
  })
}

export const getDishList = (merchantId) => {
  return request({
    url: `/student/merchant/${merchantId}/dishes`,
    method: 'GET'
  })
}
