<template>
  <view class="container">
    <view class="review-form">
      <view class="rating-item">
        <text class="label">菜品评分</text>
        <u-rate v-model="form.foodRating" :count="5" active-color="#ff6b00"></u-rate>
      </view>
      <view class="rating-item">
        <text class="label">配送评分</text>
        <u-rate v-model="form.deliveryRating" :count="5" active-color="#ff6b00"></u-rate>
      </view>
      <view class="content-item">
        <u-textarea v-model="form.content" placeholder="说说你的用餐体验..." maxlength="200"></u-textarea>
      </view>
      <button type="primary" @click="handleSubmit" :loading="loading">提交评价</button>
    </view>

    <StudentTabBarOverlay active="order" />
  </view>
</template>

<script>
import { submitReview } from '@/api/review'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      orderId: '',
      form: { foodRating: 5, deliveryRating: 5, content: '' },
      loading: false
    }
  },
  onLoad(options) {
    this.orderId = options.orderId
  },
  methods: {
    async handleSubmit() {
      this.loading = true
      try {
        await submitReview({
          orderId: this.orderId,
          ...this.form
        })
        uni.showToast({ title: '评价成功' })
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container { padding: 30rpx; background: #f8f8f8; min-height: 100vh; padding-bottom: 120rpx; }
.review-form {
  background: #fff; border-radius: 16rpx; padding: 30rpx;
  .rating-item {
    display: flex; align-items: center; margin-bottom: 30rpx;
    .label { font-size: 28rpx; margin-right: 20rpx; width: 150rpx; }
  }
  .content-item { margin-bottom: 30rpx; }
  button { margin-top: 20rpx; background: #ff6b00; }
}
</style>
