import request from '@/utils/request'

export const getAvailableTasks = () => {
  return request({
    url: '/rider/task/available',
    method: 'GET'
  })
}

export const grabTask = (id) => {
  return request({
    url: `/rider/task/${id}/grab`,
    method: 'POST'
  })
}

export const deliverTask = (id) => {
  return request({
    url: `/rider/task/${id}/deliver`,
    method: 'POST'
  })
}

export const getCurrentTask = () => {
  return request({
    url: '/rider/task/current',
    method: 'GET'
  })
}

export const getHistory = () => {
  return request({
    url: '/rider/task/history',
    method: 'GET'
  })
}
