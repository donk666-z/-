import request from '@/utils/request'

export const getShopInfo = () => {
  return request({
    url: '/merchant/shop/info',
    method: 'GET'
  })
}

export const updateShopInfo = (data) => {
  return request({
    url: '/merchant/shop/info',
    method: 'PUT',
    data
  })
}

export const updateStatus = (data) => {
  return request({
    url: '/merchant/shop/status',
    method: 'PUT',
    data
  })
}
