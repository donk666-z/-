import request from '@/utils/request'

export function getCategoryList() {
  return request({
    url: '/merchant/category/list',
    method: 'GET'
  })
}

export function createCategory(data) {
  return request({
    url: '/merchant/category/add',
    method: 'POST',
    data
  })
}

export function updateCategory(id, data) {
  return request({
    url: `/merchant/category/${id}`,
    method: 'PUT',
    data
  })
}

export function deleteCategory(id) {
  return request({
    url: `/merchant/category/${id}`,
    method: 'DELETE'
  })
}