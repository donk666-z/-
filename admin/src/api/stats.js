import request from './request'

export function getOverview() {
  return request.get('/stats/overview')
}
