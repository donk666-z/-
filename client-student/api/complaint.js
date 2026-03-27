import request from '@/utils/request'

export const getComplaintList = () => {
  return request({
    url: '/student/complaint/list',
    method: 'GET'
  })
}

export const addComplaint = (data) => {
  return request({
    url: '/student/complaint/add',
    method: 'POST',
    data
  })
}
