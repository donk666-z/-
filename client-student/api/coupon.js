import request from '@/utils/request'

export const getCouponList = () => {
  return request({
    url: '/student/coupon/list',
    method: 'GET'
  })
}

export const getCouponCenter = () => {
  return request({
    url: '/student/coupon/center',
    method: 'GET'
  })
}

export const claimCoupon = (promotionId) => {
  return request({
    url: `/student/coupon/claim/${promotionId}`,
    method: 'POST'
  })
}
