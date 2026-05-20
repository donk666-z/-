import request from './request'

export function getOverview() {
  return request.get('/admin/stats/overview')
}

export function getTrend(days = 7) {
  return request.get('/admin/stats/trend', {
    params: { days }
  })
}
