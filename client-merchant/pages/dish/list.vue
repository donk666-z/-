<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-main">
        <text class="hero-title">菜品管理</text>
        <text class="hero-subtitle">统一管理单品和套餐，随时控制上架状态。</text>
      </view>
      <view class="hero-action" @click="goAdd">
        <u-icon name="plus" size="28" color="#ffffff"></u-icon>
        <text class="hero-action-text">新增菜品</text>
      </view>
    </view>

    <view class="overview-grid">
      <view class="overview-card">
        <text class="overview-value">{{ dishList.length }}</text>
        <text class="overview-label">菜品总数</text>
      </view>
      <view class="overview-card">
        <text class="overview-value">{{ categoryGroups.length }}</text>
        <text class="overview-label">分类数量</text>
      </view>
      <view class="overview-card highlight">
        <text class="overview-value">{{ availableCount }}</text>
        <text class="overview-label">当前在售</text>
      </view>
    </view>

    <view v-if="dishList.length === 0" class="empty-wrap">
      <u-empty text="还没有菜品，先去新增一个吧" mode="data"></u-empty>
    </view>

    <view v-else class="category-list">
      <view v-for="group in categoryGroups" :key="group.name" class="category-section">
        <view class="category-header">
          <view class="category-title-wrap">
            <text class="category-title">{{ group.name }}</text>
            <text class="category-meta">{{ group.dishes.length }} 道菜</text>
          </view>
          <text class="category-badge">{{ getAvailableCount(group.dishes) }} 在售</text>
        </view>

        <view v-for="dish in group.dishes" :key="dish.id" class="dish-card">
          <image v-if="dish.image" :src="dish.image" class="dish-image" mode="aspectFill"></image>
          <view v-else class="dish-image dish-image-placeholder">
            <u-icon name="photo" size="34" color="#c1c7d0"></u-icon>
          </view>

          <view class="dish-main">
            <view class="dish-top">
              <text class="dish-name">{{ dish.name }}</text>
              <text class="dish-type">{{ dish.type === 'combo' ? '套餐' : '单品' }}</text>
              <text class="dish-status" :class="dish.available ? 'on' : 'off'">
                {{ dish.available ? '已上架' : '已下架' }}
              </text>
            </view>

            <text class="dish-desc">{{ dish.description || '暂无菜品描述' }}</text>

            <view class="dish-bottom">
              <view class="price-box">
                <text class="price-label">价格</text>
                <text class="dish-price">{{ formatPrice(dish.price) }}</text>
              </view>
              <text class="dish-stock">{{ dish.type === 'combo' ? `动态库存 ${dish.stock}` : `库存 ${dish.stock}` }}</text>
            </view>
          </view>

          <view class="dish-side">
            <u-switch
              v-model="dish.available"
              activeColor="#ff7a1a"
              inactiveColor="#d7dce3"
              size="40"
              @change="toggleStatus(dish)"
            ></u-switch>

            <view class="action-group">
              <view class="action-btn edit" @click="goEdit(dish.id)">
                <u-icon name="edit-pen" size="24" color="#4a5568"></u-icon>
              </view>
              <view class="action-btn delete" @click="handleDelete(dish)">
                <u-icon name="trash" size="24" color="#ff5a5f"></u-icon>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getDishList, deleteDish, updateDishStatus } from '@/api/dish'

export default {
  data() {
    return {
      dishList: []
    }
  },
  computed: {
    categoryGroups() {
      const groups = []
      const groupMap = {}

      this.dishList.forEach((dish) => {
        const categoryName = dish.category || '未分类'
        if (!groupMap[categoryName]) {
          groupMap[categoryName] = {
            name: categoryName,
            dishes: []
          }
          groups.push(groupMap[categoryName])
        }
        groupMap[categoryName].dishes.push(dish)
      })

      return groups
    },
    availableCount() {
      return this.dishList.filter((dish) => dish.status === 'available').length
    }
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.loadDishes()
  },
  methods: {
    async loadDishes() {
      try {
        const res = await getDishList()
        this.dishList = (res.records || res || []).map((dish) => ({
          ...dish,
          type: dish.type === 'combo' ? 'combo' : 'single',
          available: dish.status === 'available'
        }))
      } catch (error) {
        console.error('加载菜品失败', error)
      }
    },
    formatPrice(price) {
      const value = Number(price || 0)
      return value.toFixed(2)
    },
    getAvailableCount(dishes) {
      return dishes.filter((dish) => dish.status === 'available').length
    },
    async toggleStatus(dish) {
      try {
        const nextStatus = dish.available ? 'available' : 'unavailable'
        await updateDishStatus(dish.id, { status: nextStatus })
        dish.status = nextStatus
        uni.showToast({
          title: dish.available ? '已上架' : '已下架',
          icon: 'none'
        })
      } catch (error) {
        dish.available = !dish.available
        console.error('更新菜品状态失败', error)
      }
    },
    handleDelete(dish) {
      uni.showModal({
        title: '删除确认',
        content: `确定删除 ${dish.name} 吗？`,
        success: async (res) => {
          if (!res.confirm) return

          try {
            await deleteDish(dish.id)
            uni.showToast({ title: '删除成功', icon: 'none' })
            this.loadDishes()
          } catch (error) {
            console.error('删除菜品失败', error)
          }
        }
      })
    },
    goAdd() {
      uni.navigateTo({ url: '/pages/dish/edit' })
    },
    goEdit(id) {
      uni.navigateTo({ url: `/pages/dish/edit?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #fff8f2 0%, #f6f7fb 240rpx, #f6f7fb 100%);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #ff9a3d 0%, #ff6b00 100%);
  box-shadow: 0 20rpx 44rpx rgba(255, 107, 0, 0.22);
}

.hero-main {
  flex: 1;
  min-width: 0;
  padding-right: 20rpx;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.9);
}

.hero-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  flex-shrink: 0;
  min-width: 188rpx;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.2);
  border: 2rpx solid rgba(255, 255, 255, 0.24);
}

.hero-action-text {
  font-size: 26rpx;
  font-weight: 600;
  color: #ffffff;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 24rpx;
}

.overview-card {
  padding: 24rpx 18rpx;
  border-radius: 22rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 30rpx rgba(22, 35, 66, 0.06);
}

.overview-card.highlight {
  background: linear-gradient(135deg, #fff4e8 0%, #ffffff 100%);
}

.overview-value {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
}

.overview-label {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8b93a6;
}

.empty-wrap {
  margin-top: 32rpx;
  padding: 60rpx 0;
  border-radius: 26rpx;
  background: #ffffff;
}

.category-list {
  margin-top: 28rpx;
}

.category-section {
  margin-bottom: 24rpx;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.category-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 14rpx;
  min-width: 0;
}

.category-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.category-meta {
  font-size: 22rpx;
  color: #9aa3b2;
}

.category-badge {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #fff1e6;
  font-size: 22rpx;
  color: #ff7a1a;
}

.dish-card {
  display: flex;
  align-items: center;
  padding: 22rpx;
  margin-bottom: 16rpx;
  border-radius: 24rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 32rpx rgba(18, 38, 63, 0.06);
}

.dish-image {
  width: 136rpx;
  height: 136rpx;
  margin-right: 22rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
  background: #f4f6fa;
}

.dish-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-main {
  flex: 1;
  min-width: 0;
}

.dish-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.dish-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 700;
  color: #202939;
}

.dish-type {
  flex-shrink: 0;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #fff5ea;
  color: #ff7a1a;
  font-size: 20rpx;
}

.dish-status {
  flex-shrink: 0;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.dish-status.on {
  background: rgba(22, 187, 110, 0.12);
  color: #19be6b;
}

.dish-status.off {
  background: #f1f4f8;
  color: #8e97a8;
}

.dish-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #8b93a6;
}

.dish-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 18rpx;
  gap: 20rpx;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}

.price-label {
  font-size: 22rpx;
  color: #9aa3b2;
}

.dish-price {
  font-size: 34rpx;
  font-weight: 700;
  color: #ff6b00;
}

.dish-stock {
  font-size: 24rpx;
  color: #7b8495;
}

.dish-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
  margin-left: 18rpx;
}

.action-group {
  display: flex;
  gap: 14rpx;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  border-radius: 18rpx;
  background: #f7f8fb;
}

.action-btn.delete {
  background: #fff1f1;
}
</style>
