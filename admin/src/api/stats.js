import request from './request'

export function getOverview() {
  return request.get('/admin/stats/overview')
}
