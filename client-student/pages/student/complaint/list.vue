<template>
  <view class="container">
    <view class="complaint-form">
      <view class="form-item">
        <text class="label">{{ text.typeLabel }}</text>
        <picker :value="complaintTypeIndex" :range="complaintTypes" @change="onTypeChange">
          <view class="picker-value">{{ complaintTypes[complaintTypeIndex] }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">{{ text.contentLabel }}</text>
        <textarea
          class="textarea"
          v-model="content"
          :placeholder="text.contentPlaceholder"
          maxlength="500"
        ></textarea>
      </view>
      <view class="form-item">
        <text class="label">{{ text.contactLabel }}</text>
        <input class="input" v-model="contact" :placeholder="text.contactPlaceholder" />
      </view>
      <button class="submit-btn" @click="submitComplaint">{{ text.submit }}</button>
    </view>

    <view class="history-section">
      <view class="section-title">{{ text.historyTitle }}</view>
      <view class="loading-wrap" v-if="loading">
        <u-loading-icon></u-loading-icon>
      </view>
      <view class="empty-wrap" v-else-if="complaintList.length === 0">
        <text class="empty-text">{{ text.empty }}</text>
      </view>
      <view
        class="complaint-item"
        v-else
        v-for="item in complaintList"
        :key="item.id"
        @click="openDetail(item)"
      >
        <view class="complaint-header">
          <text class="complaint-type">{{ item.type }}</text>
          <text class="complaint-status" :class="item.status">{{ statusText(item.status) }}</text>
        </view>
        <text class="complaint-content">{{ item.content }}</text>
        <view class="complaint-footer">
          <text class="complaint-time">{{ item.createdAt }}</text>
          <text class="detail-link">{{ text.viewDetail }}</text>
        </view>
      </view>
    </view>

    <u-popup :show="detailVisible" mode="bottom" round="20" @close="closeDetail">
      <view class="detail-popup" v-if="currentComplaint">
        <view class="popup-head">
          <text class="popup-title">{{ text.detailTitle }}</text>
          <text class="popup-close" @click="closeDetail">{{ text.close }}</text>
        </view>

        <view class="detail-card">
          <view class="detail-row">
            <text class="detail-label">{{ text.typeLabel }}</text>
            <text class="detail-value">{{ currentComplaint.type }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">{{ text.statusLabel }}</text>
            <text class="detail-value status" :class="currentComplaint.status">{{ statusText(currentComplaint.status) }}</text>
          </view>
          <view class="detail-block">
            <text class="detail-label">{{ text.contentLabel }}</text>
            <text class="detail-text">{{ currentComplaint.content || '-' }}</text>
          </view>
          <view class="detail-block">
            <text class="detail-label">{{ text.replyLabel }}</text>
            <text class="detail-text">{{ currentComplaint.reply || text.noReply }}</text>
          </view>
          <view class="detail-row" v-if="currentComplaint.contact">
            <text class="detail-label">{{ text.contactLabel }}</text>
            <text class="detail-value">{{ currentComplaint.contact }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">{{ text.timeLabel }}</text>
            <text class="detail-value">{{ currentComplaint.createdAt }}</text>
          </view>
        </view>
      </view>
    </u-popup>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'
import { addComplaint, getComplaintList } from '@/api/complaint'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      text: {
        typeLabel: '\u6295\u8bc9\u7c7b\u578b',
        contentLabel: '\u6295\u8bc9\u5185\u5bb9',
        contentPlaceholder: '\u8bf7\u8be6\u7ec6\u63cf\u8ff0\u60a8\u7684\u95ee\u9898...',
        contactLabel: '\u8054\u7cfb\u65b9\u5f0f',
        contactPlaceholder: '\u8bf7\u8f93\u5165\u624b\u673a\u53f7',
        submit: '\u63d0\u4ea4\u6295\u8bc9',
        historyTitle: '\u6295\u8bc9\u8bb0\u5f55',
        empty: '\u6682\u65e0\u6295\u8bc9\u8bb0\u5f55',
        pending: '\u5904\u7406\u4e2d',
        resolved: '\u5df2\u89e3\u51b3',
        closed: '\u5df2\u5173\u95ed',
        viewDetail: '\u67e5\u770b\u8be6\u60c5',
        detailTitle: '\u6295\u8bc9\u8be6\u60c5',
        close: '\u5173\u95ed',
        statusLabel: '\u5904\u7406\u72b6\u6001',
        replyLabel: '\u5904\u7406\u56de\u590d',
        noReply: '\u6682\u65e0\u56de\u590d',
        timeLabel: '\u63d0\u4ea4\u65f6\u95f4',
        submitSuccess: '\u63d0\u4ea4\u6210\u529f',
        submitFail: '\u63d0\u4ea4\u5931\u8d25',
        emptyContent: '\u8bf7\u8f93\u5165\u6295\u8bc9\u5185\u5bb9'
      },
      complaintTypes: [
        '\u5546\u54c1\u95ee\u9898',
        '\u914d\u9001\u95ee\u9898',
        '\u670d\u52a1\u6001\u5ea6',
        '\u5176\u4ed6'
      ],
      complaintTypeIndex: 0,
      content: '',
      contact: '',
      complaintList: [],
      loading: false,
      detailVisible: false,
      currentComplaint: null
    }
  },
  onLoad() {
    this.loadComplaintList()
  },
  methods: {
    statusText(status) {
      if (status === 'resolved') return this.text.resolved
      if (status === 'closed') return this.text.closed
      return this.text.pending
    },
    onTypeChange(e) {
      this.complaintTypeIndex = Number(e.detail.value || 0)
    },
    async submitComplaint() {
      if (!this.content.trim()) {
        uni.showToast({ title: this.text.emptyContent, icon: 'none' })
        return
      }
      try {
        await addComplaint({
          type: this.complaintTypes[this.complaintTypeIndex],
          content: this.content,
          contact: this.contact
        })
        uni.showToast({ title: this.text.submitSuccess, icon: 'success' })
        this.content = ''
        this.contact = ''
        this.loadComplaintList()
      } catch (e) {
        uni.showToast({ title: this.text.submitFail, icon: 'none' })
      }
    },
    async loadComplaintList() {
      this.loading = true
      try {
        const list = await getComplaintList()
        this.complaintList = Array.isArray(list) ? list : []
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    openDetail(item) {
      this.currentComplaint = item
      this.detailVisible = true
    },
    closeDetail() {
      this.detailVisible = false
      this.currentComplaint = null
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  background: #f8f8f8;
  min-height: 100vh;
  padding: 20rpx;
  padding-bottom: 120rpx;
}

.complaint-form {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.picker-value,
.textarea,
.input {
  background: #f8f8f8;
  border-radius: 8rpx;
  font-size: 28rpx;
}

.picker-value,
.input {
  padding: 20rpx;
}

.textarea {
  padding: 20rpx;
  width: 100%;
  box-sizing: border-box;
  height: 200rpx;
}

.submit-btn {
  background: #ff6b00;
  color: #fff;
  border-radius: 50rpx;
  margin-top: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.complaint-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.complaint-header,
.complaint-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.complaint-header {
  margin-bottom: 12rpx;
}

.complaint-type {
  font-size: 28rpx;
  font-weight: bold;
}

.complaint-status {
  font-size: 24rpx;
  color: #ff9900;
}

.complaint-status.resolved {
  color: #19be6b;
}

.complaint-status.closed {
  color: #999;
}

.complaint-content {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 12rpx;
}

.complaint-time,
.detail-link {
  font-size: 22rpx;
  color: #999;
}

.detail-link {
  color: #ff6b00;
}

.detail-popup {
  padding: 30rpx;
}

.popup-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 600;
}

.popup-close {
  font-size: 26rpx;
  color: #999;
}

.detail-card {
  background: #fff;
  border-radius: 16rpx;
}

.detail-row,
.detail-block {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2f2f2;
}

.detail-row:last-child,
.detail-block:last-child {
  border-bottom: none;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.detail-label {
  font-size: 26rpx;
  color: #999;
}

.detail-value,
.detail-text {
  font-size: 26rpx;
  color: #333;
}

.detail-value.status.pending {
  color: #ff9900;
}

.detail-value.status.resolved {
  color: #19be6b;
}

.detail-value.status.closed {
  color: #999;
}

.detail-text {
  display: block;
  margin-top: 12rpx;
  line-height: 1.7;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-wrap {
  text-align: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
