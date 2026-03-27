import request from '@/utils/request'

export const getDishList = (params) => {
  return request({
    url: '/merchant/dish/list',
    method: 'GET',
    data: params
  })
}

export const getDishDetail = (id) => {
  return request({
    url: `/merchant/dish/${id}`,
    method: 'GET'
  })
}

export const createDish = (data) => {
  return request({
    url: '/merchant/dish',
    method: 'POST',
    data
  })
}

export const updateDish = (id, data) => {
  return request({
    url: `/merchant/dish/${id}`,
    method: 'PUT',
    data
  })
}

export const deleteDish = (id) => {
  return request({
    url: `/merchant/dish/${id}`,
    method: 'DELETE'
  })
}

export const updateDishStatus = (id, data) => {
  return request({
    url: `/merchant/dish/${id}/status`,
    method: 'PUT',
    data
  })
}
