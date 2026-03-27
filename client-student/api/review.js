import request from '@/utils/request'

export const submitReview = (data) => {
  return request({
    url: '/student/review',
    method: 'POST',
    data
  })
}

export const getMerchantReviews = (merchantId) => {
  return request({
    url: `/student/review/merchant/${merchantId}`,
    method: 'GET'
  })
}
