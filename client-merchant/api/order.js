import request from '@/utils/request'

export const getOrderList = (params) => {
  return request({
    url: '/merchant/order/list',
    method: 'GET',
    data: params
  })
}

export const getOrderDetail = (id) => {
  return request({
    url: `/merchant/order/${id}`,
    method: 'GET'
  })
}

export const getPendingOrders = () => {
  return request({
    url: '/merchant/order/pending',
    method: 'GET'
  })
}

export const acceptOrder = (id) => {
  return request({
    url: `/merchant/order/${id}/accept`,
    method: 'POST'
  })
}

export const prepareOrder = (id) => {
  return request({
    url: `/merchant/order/${id}/prepare`,
    method: 'POST'
  })
}
