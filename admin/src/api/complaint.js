import request from './request'

export function getComplaintList(params) {
  return request.get('/admin/complaint/list', { params })
}

export function replyComplaint(id, data) {
  return request.put(`/admin/complaint/${id}/reply`, data)
}

