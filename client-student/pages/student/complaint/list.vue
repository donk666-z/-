<template>
  <view class="container">
    <view class="complaint-form">
      <view class="form-item">
        <text class="label">投诉类型</text>
        <picker :value="complaintTypeIndex" :range="complaintTypes" @change="onTypeChange">
          <view class="picker-value">{{ complaintTypes[complaintTypeIndex] }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">投诉内容</text>
        <textarea class="textarea" v-model="content" placeholder="请详细描述您的问题..." maxlength="500"></textarea>
      </view>
      <view class="form-item">
        <text class="label">联系方式</text>
        <input class="input" v-model="contact" placeholder="请输入手机号" />
      </view>
      <button class="submit-btn" @click="submitComplaint">提交投诉</button>
    </view>

    <view class="history-section">
      <view class="section-title">投诉记录</view>
      <view class="loading-wrap" v-if="loading">
        <u-loading-icon></u-loading-icon>
      </view>
      <view class="empty-wrap" v-else-if="complaintList.length === 0">
        <text class="empty-text">暂无投诉记录</text>
      </view>
      <view class="complaint-item" v-else v-for="item in complaintList" :key="item.id">
        <view class="complaint-header">
          <text class="complaint-type">{{ item.type }}</text>
          <text class="complaint-status" :class="item.status">{{ item.status === 'pending' ? '处理中' : item.status === 'resolved' ? '已解决' : '已关闭' }}</text>
        </view>
        <text class="complaint-content">{{ item.content }}</text>
        <text class="complaint-time">{{ item.createdAt }}</text>
      </view>
    </view>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import { getComplaintList, addComplaint } from '@/api/complaint'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      complaintTypes: ['商品问题', '配送问题', '服务态度', '其他'],
      complaintTypeIndex: 0,
      content: '',
      contact: '',
      complaintList: [],
      loading: false
    }
  },
  onLoad() {
    this.loadComplaintList()
  },
  methods: {
    onTypeChange(e) {
      this.complaintTypeIndex = e.detail.value
    },
    async submitComplaint() {
      if (!this.content.trim()) {
        uni.showToast({ title: '请输入投诉内容', icon: 'none' })
        return
      }
      try {
        await addComplaint({
          type: this.complaintTypes[this.complaintTypeIndex],
          content: this.content,
          contact: this.contact
        })
        uni.showToast({ title: '提交成功' })
        this.content = ''
        this.contact = ''
        this.loadComplaintList()
      } catch (e) {
        uni.showToast({ title: '提交失败', icon: 'none' })
      }
    },
    async loadComplaintList() {
      this.loading = true
      try {
        const list = await getComplaintList()
        this.complaintList = list || []
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
.container { background: #f8f8f8; min-height: 100vh; padding: 20rpx; padding-bottom: 120rpx; }
.complaint-form {
  background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 30rpx;
  .form-item { margin-bottom: 30rpx;
    .label { display: block; font-size: 28rpx; font-weight: bold; margin-bottom: 16rpx; }
    .picker-value { background: #f8f8f8; padding: 20rpx; border-radius: 8rpx; font-size: 28rpx; }
    .textarea { background: #f8f8f8; padding: 20rpx; border-radius: 8rpx; font-size: 28rpx; width: 100%; box-sizing: border-box; height: 200rpx; }
    .input { background: #f8f8f8; padding: 20rpx; border-radius: 8rpx; font-size: 28rpx; }
  }
  .submit-btn { background: #ff6b00; color: #fff; border-radius: 50rpx; margin-top: 20rpx; }
}
.history-section {
  .section-title { font-size: 32rpx; font-weight: bold; margin-bottom: 20rpx; }
  .complaint-item { background: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 20rpx;
    .complaint-header { display: flex; justify-content: space-between; margin-bottom: 12rpx;
      .complaint-type { font-size: 28rpx; font-weight: bold; }
      .complaint-status { font-size: 24rpx; color: #ff9900; &.resolved { color: #19be6b; } &.closed { color: #999; } }
    }
    .complaint-content { font-size: 26rpx; color: #666; display: block; margin-bottom: 12rpx; }
    .complaint-time { font-size: 22rpx; color: #999; }
  }
}
.loading-wrap { display: flex; justify-content: center; padding: 100rpx 0; }
.empty-wrap { text-align: center; padding: 100rpx 0; .empty-text { font-size: 28rpx; color: #999; } }
</style>
