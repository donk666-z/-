<template>
  <view class="container">
    <view class="about-header">
      <image class="app-logo" src="/static/default-avatar.png" mode="aspectFill"></image>
      <text class="app-name">校园外卖</text>
      <text class="app-version">Version 1.0.0</text>
    </view>

    <view class="about-section">
      <view class="section-title">关于我们</view>
      <view class="about-content">
        <text class="paragraph">校园外卖是一款专为校园场景设计的外卖订餐平台，致力于为广大师生提供便捷、快速、优惠的餐饮服务。</text>
        <text class="paragraph">我们与校园周边优质商家合作，提供丰富的菜品选择，快速的配送服务，让同学们足不出户就能享受到美味佳肴。</text>
      </view>
    </view>

    <view class="about-section">
      <view class="section-title">联系方式</view>
      <view class="contact-item">
        <u-icon name="phone" size="20" color="#ff6b00"></u-icon>
        <text class="contact-text">客服电话：400-123-4567</text>
      </view>
      <view class="contact-item">
        <u-icon name="email" size="20" color="#ff6b00"></u-icon>
        <text class="contact-text">邮箱：support@campusdelivery.com</text>
      </view>
      <view class="contact-item">
        <u-icon name="map" size="20" color="#ff6b00"></u-icon>
        <text class="contact-text">地址：扬州大学校园</text>
      </view>
    </view>

    <view class="about-section">
      <view class="section-title">功能特点</view>
      <view class="feature-item">
        <u-icon name="checkmark-circle" size="20" color="#19be6b"></u-icon>
        <text class="feature-text">海量商家，品类丰富</text>
      </view>
      <view class="feature-item">
        <u-icon name="checkmark-circle" size="20" color="#19be6b"></u-icon>
        <text class="feature-text">快速配送，准时送达</text>
      </view>
      <view class="feature-item">
        <u-icon name="checkmark-circle" size="20" color="#19be6b"></u-icon>
        <text class="feature-text">优惠活动，实惠多多</text>
      </view>
      <view class="feature-item">
        <u-icon name="checkmark-circle" size="20" color="#19be6b"></u-icon>
        <text class="feature-text">评价系统，真实可靠</text>
      </view>
    </view>

    <view class="copyright">
      <text>© 2024 校园外卖 版权所有</text>
    </view>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay }
}
</script>

<style lang="scss" scoped>
.container { background: #f8f8f8; min-height: 100vh; padding-bottom: 120rpx; }
.about-header {
  background: linear-gradient(135deg, #ff6b00, #ff9900); color: #fff;
  padding: 60rpx 30rpx; text-align: center;
  .app-logo { width: 160rpx; height: 160rpx; border-radius: 32rpx; margin-bottom: 20rpx; }
  .app-name { display: block; font-size: 40rpx; font-weight: bold; margin-bottom: 8rpx; }
  .app-version { font-size: 24rpx; opacity: 0.8; }
}
.about-section {
  background: #fff; margin: 20rpx; border-radius: 16rpx; padding: 30rpx;
  .section-title { font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }
  .about-content {
    .paragraph { font-size: 28rpx; color: #666; line-height: 1.8; display: block; margin-bottom: 16rpx; }
  }
  .contact-item { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .contact-text { font-size: 28rpx; color: #333; margin-left: 16rpx; }
  }
  .feature-item { display: flex; align-items: center; padding: 12rpx 0;
    .feature-text { font-size: 28rpx; color: #333; margin-left: 16rpx; }
  }
}
.copyright { text-align: center; padding: 40rpx; font-size: 24rpx; color: #999; }
</style>
