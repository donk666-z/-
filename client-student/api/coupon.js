import request from '@/utils/request'

export const getCouponList = () => {
  return request({
    url: '/student/coupon/list',
    method: 'GET'
  })
}
