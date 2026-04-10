<template>
  <view class="container">
    <view class="hero-card">
      <text class="hero-title">{{ isEdit ? '编辑菜品' : '新增菜品' }}</text>
      <text class="hero-subtitle">
        {{ form.type === 'combo' ? '套餐按已有菜品分类配置，学生下单时需在每个分类中选择指定商品。' : '单品会直接在学生端展示并按库存售卖。' }}
      </text>
    </view>

    <view class="form-card">
      <view class="section-header">
        <text class="section-title">基础信息</text>
        <text class="section-tip">带 * 为必填项</text>
      </view>

      <view class="type-switch">
        <view class="type-chip" :class="{ active: form.type === 'single' }" @click="setDishType('single')">单品</view>
        <view class="type-chip" :class="{ active: form.type === 'combo' }" @click="setDishType('combo')">套餐</view>
      </view>

      <u-form :model="form" ref="formRef" labelWidth="160rpx">
        <u-form-item label="菜品名称" prop="name" required>
          <u-input v-model="form.name" placeholder="请输入菜品或套餐名称"></u-input>
        </u-form-item>
        <u-form-item label="分类名称" prop="category" required>
          <u-input v-model="form.category" placeholder="例如：主食、饮品、套餐"></u-input>
        </u-form-item>
        <u-form-item label="价格" prop="price" required>
          <u-input v-model="form.price" type="digit" placeholder="请输入售价"></u-input>
        </u-form-item>
        <u-form-item v-if="form.type === 'single'" label="库存" prop="stock" required>
          <u-input v-model="form.stock" type="number" placeholder="请输入库存数量"></u-input>
        </u-form-item>
        <view v-else class="combo-tip">套餐库存会根据所选分类下可售单品自动计算，无需手动填写。</view>
        <u-form-item label="菜品描述" prop="description">
          <u-input v-model="form.description" type="textarea" autoHeight placeholder="简单介绍卖点、口味或适合人群"></u-input>
        </u-form-item>
      </u-form>
    </view>

    <view class="form-card" v-if="form.type === 'combo'">
      <view class="section-header">
        <text class="section-title">套餐规则</text>
        <text class="section-tip">每个分类当前固定选择 1 个单品</text>
      </view>

      <view v-if="comboCategoryOptions.length === 0" class="empty-config">请先创建至少一个包含单品的菜品分类，再回来配置套餐。</view>

      <view v-for="(rule, ruleIndex) in form.comboConfig.rules" :key="ruleIndex" class="combo-group">
        <view class="group-header">
          <text class="group-title">分类规则 {{ ruleIndex + 1 }}</text>
          <text class="group-remove" @click="removeComboRule(ruleIndex)">删除规则</text>
        </view>

        <view class="field-block">
          <text class="field-label">选择分类</text>
          <picker class="picker-wrap" :range="comboCategoryOptions" range-key="name" :value="getCategoryPickerIndex(rule.categoryId)" @change="onSelectComboCategory(ruleIndex, $event)">
            <view class="picker-value">{{ getCategoryLabel(rule.categoryId) || '请选择分类' }}</view>
          </picker>
        </view>

        <view class="inline-fields single-column">
          <view class="inline-field">
            <text class="field-label">选择数量</text>
            <input v-model="rule.requiredCount" class="field-input" type="number" disabled />
          </view>
        </view>
      </view>

      <view class="add-group-btn" @click="addComboRule">+ 添加分类规则</view>
    </view>

    <view class="form-card">
      <view class="section-header">
        <text class="section-title">菜品图片</text>
        <text class="section-tip">建议上传清晰正方形图片</text>
      </view>

      <view class="upload-panel" @click="chooseImage">
        <image v-if="form.image" :src="form.image" class="preview-img" mode="aspectFill"></image>
        <view v-else class="upload-placeholder">
          <u-icon name="camera-fill" size="44" color="#ff7a1a"></u-icon>
          <text class="upload-title">上传菜品图片</text>
          <text class="upload-desc">支持拍照或从相册选择</text>
        </view>

        <view v-if="form.image" class="upload-mask">
          <u-icon name="edit-pen" size="28" color="#ffffff"></u-icon>
          <text class="upload-mask-text">重新上传</text>
        </view>
      </view>
    </view>

    <view class="tips-card">
      <text class="tips-title">填写提示</text>
      <text class="tips-item">1. 套餐价格填写套餐固定价，学生选完各分类商品后仍按套餐价结算。</text>
      <text class="tips-item">2. 同一个套餐内分类不能重复，且分类必须属于当前商家。</text>
      <text class="tips-item">3. 套餐只能引用单品分类，不支持在套餐中再嵌套套餐。</text>
    </view>

    <view class="bottom-bar">
      <u-button type="primary" shape="circle" @click="handleSubmit" :loading="loading">{{ isEdit ? '保存修改' : '确认新增' }}</u-button>
    </view>
  </view>
</template>

<script>
import { getDishDetail, createDish, updateDish, getDishList } from '@/api/dish'
import { getCategoryList } from '@/api/category'
import { UPLOAD_URL } from '@/config/api'
import { handleUnauthorized } from '@/utils/session'

const emptyRule = () => ({
  categoryId: null,
  categoryName: '',
  requiredCount: '1'
})

export default {
  data() {
    return {
      isEdit: false,
      dishId: '',
      loading: false,
      categoryOptions: [],
      comboCategoryOptions: [],
      form: {
        type: 'single',
        name: '',
        description: '',
        price: '',
        stock: '',
        category: '',
        image: '',
        comboConfig: {
          rules: []
        }
      }
    }
  },
  onLoad(options) {
    this.dishId = options.id || ''
    this.isEdit = !!options.id
    uni.setNavigationBarTitle({ title: this.isEdit ? '编辑菜品' : '新增菜品' })
    this.bootstrap()
  },
  methods: {
    normalizeDishType(type) {
      return type === 'combo' ? 'combo' : 'single'
    },
    async bootstrap() {
      // loadSelectableDishes 依赖 categoryOptions，需保证分类先加载完成
      await this.loadCategories()
      await this.loadSelectableDishes()
      if (this.isEdit) {
        await this.loadDish()
      }
      if (this.form.type === 'combo' && this.form.comboConfig.rules.length === 0) {
        this.addComboRule()
      }
    },
    async loadCategories() {
      try {
        const list = await getCategoryList()
        this.categoryOptions = Array.isArray(list) ? list : []
      } catch (error) {
        console.error('加载分类失败', error)
        this.categoryOptions = []
      }
    },
    async loadSelectableDishes() {
      try {
        const response = await getDishList()
        const list = (response.records || response || []).map((dish) => ({
          id: Number(dish.id),
          type: this.normalizeDishType(dish.type),
          categoryId: dish.categoryId !== undefined && dish.categoryId !== null ? Number(dish.categoryId) : null
        }))
        const categoryIdSet = new Set(
          list
            .filter((dish) => dish.type === 'single' && String(dish.id) !== String(this.dishId) && dish.categoryId)
            .map((dish) => dish.categoryId)
        )
        this.comboCategoryOptions = this.categoryOptions.filter((category) => categoryIdSet.has(Number(category.id)))
      } catch (error) {
        console.error('加载单品失败', error)
        this.comboCategoryOptions = []
      }
    },
    async loadDish() {
      try {
        const dish = await getDishDetail(this.dishId)
        if (!dish) return

        this.form = {
          type: this.normalizeDishType(dish.type),
          name: dish.name || '',
          description: dish.description || '',
          price: String(dish.price || ''),
          stock: String(dish.stock || ''),
          category: dish.category || '',
          image: dish.image || '',
          comboConfig: {
            rules: this.normalizeComboRules(dish.comboConfig && dish.comboConfig.rules)
          }
        }
      } catch (error) {
        console.error('加载菜品详情失败', error)
      }
    },
    normalizeComboRules(rules) {
      if (!Array.isArray(rules)) {
        return []
      }
      return rules.map((rule) => ({
        categoryId: rule.categoryId || null,
        categoryName: rule.categoryName || '',
        requiredCount: String(rule.requiredCount || 1)
      }))
    },
    setDishType(type) {
      this.form.type = type
      if (type === 'combo' && this.form.comboConfig.rules.length === 0) {
        this.addComboRule()
      }
    },
    addComboRule() {
      this.form.comboConfig.rules.push(emptyRule())
    },
    removeComboRule(ruleIndex) {
      this.form.comboConfig.rules.splice(ruleIndex, 1)
      if (this.form.comboConfig.rules.length === 0 && this.form.type === 'combo') {
        this.form.comboConfig.rules.push(emptyRule())
      }
    },
    getCategoryPickerIndex(categoryId) {
      const index = this.comboCategoryOptions.findIndex((category) => String(category.id) === String(categoryId))
      return index >= 0 ? index : 0
    },
    getCategoryLabel(categoryId) {
      const matched = this.comboCategoryOptions.find((category) => String(category.id) === String(categoryId))
      return matched ? matched.name : ''
    },
    onSelectComboCategory(ruleIndex, event) {
      const picked = this.comboCategoryOptions[Number(event.detail.value)]
      if (!picked) return
      this.form.comboConfig.rules[ruleIndex].categoryId = Number(picked.id)
      this.form.comboConfig.rules[ruleIndex].categoryName = picked.name || ''
      this.form.comboConfig.rules[ruleIndex].requiredCount = '1'
    },
    chooseImage() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const tempPath = res.tempFilePaths[0]
          uni.uploadFile({
            url: UPLOAD_URL,
            filePath: tempPath,
            name: 'file',
            header: {
              Authorization: `Bearer ${uni.getStorageSync('token')}`
            },
            success: (uploadRes) => {
              if (uploadRes.statusCode === 401) {
                uni.showToast({ title: '请先登录', icon: 'none' })
                handleUnauthorized()
                return
              }
              if (uploadRes.statusCode === 403) {
                uni.showToast({ title: '账号已被禁用，请重新登录', icon: 'none' })
                handleUnauthorized()
                return
              }
              const data = JSON.parse(uploadRes.data || '{}')
              if (data.code === 0) {
                this.form.image = data.data.url || data.data
                return
              }
              uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            },
            fail: () => {
              uni.showToast({ title: '上传失败', icon: 'none' })
            }
          })
        }
      })
    },
    buildComboPayload() {
      const rules = this.form.comboConfig.rules
      if (!Array.isArray(rules) || rules.length === 0) {
        throw new Error('请至少配置一个套餐分类')
      }
      if (this.comboCategoryOptions.length === 0) {
        throw new Error('请先创建包含单品的分类')
      }

      const seenCategoryIds = new Set()
      return {
        rules: rules.map((rule, ruleIndex) => {
          const categoryId = Number(rule.categoryId)
          const requiredCount = 1
          const category = this.comboCategoryOptions.find((item) => Number(item.id) === categoryId)

          if (!Number.isFinite(categoryId)) {
            throw new Error(`请选择第 ${ruleIndex + 1} 个分类`)
          }
          if (!category) {
            throw new Error(`第 ${ruleIndex + 1} 个分类不存在或没有可选单品`)
          }
          if (seenCategoryIds.has(categoryId)) {
            throw new Error('同一个套餐内分类不能重复')
          }
          seenCategoryIds.add(categoryId)

          return {
            categoryId,
            categoryName: category.name || rule.categoryName || '',
            requiredCount
          }
        })
      }
    },
    async handleSubmit() {
      if (!this.form.name || !this.form.price || !this.form.category) {
        return uni.showToast({ title: '请填写必填项', icon: 'none' })
      }
      if (this.form.type === 'single' && !this.form.stock) {
        return uni.showToast({ title: '请填写库存', icon: 'none' })
      }

      this.loading = true
      try {
        const data = {
          type: this.form.type,
          name: this.form.name,
          description: this.form.description,
          price: parseFloat(this.form.price),
          stock: this.form.type === 'single' ? parseInt(this.form.stock, 10) : 0,
          category: this.form.category,
          image: this.form.image
        }

        if (this.form.type === 'combo') {
          data.comboConfig = this.buildComboPayload()
        }

        if (this.isEdit) {
          await updateDish(this.dishId, data)
          uni.showToast({ title: '修改成功', icon: 'none' })
        } else {
          await createDish(data)
          uni.showToast({ title: '添加成功', icon: 'none' })
        }

        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } catch (error) {
        const message = error && error.message ? error.message : '提交失败'
        uni.showToast({ title: message, icon: 'none' })
        console.error('提交菜品失败', error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  padding: 24rpx 24rpx 180rpx;
  background: linear-gradient(180deg, #fff8f2 0%, #f6f7fb 260rpx, #f6f7fb 100%);
}
.hero-card {
  padding: 30rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #ff9b44 0%, #ff6b00 100%);
  box-shadow: 0 20rpx 44rpx rgba(255, 107, 0, 0.2);
}
.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}
.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
}
.form-card,
.tips-card {
  margin-top: 24rpx;
  padding: 28rpx 24rpx;
  border-radius: 26rpx;
  background: #ffffff;
  box-shadow: 0 12rpx 30rpx rgba(20, 34, 64, 0.06);
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #202939;
}
.section-tip {
  font-size: 22rpx;
  color: #9aa3b2;
}
.type-switch {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.type-chip {
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  background: #f3f5f8;
  color: #718096;
  font-size: 26rpx;
}
.type-chip.active {
  background: #fff0e4;
  color: #ff6b00;
  font-weight: 600;
}
.combo-tip,
.empty-config {
  margin-bottom: 18rpx;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #fff7ef;
  color: #8b5c2a;
  font-size: 24rpx;
  line-height: 1.6;
}
.combo-group {
  margin-bottom: 24rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: #fff8f2;
}
.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.group-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
}
.group-remove {
  font-size: 24rpx;
  color: #f56c6c;
}
.field-block {
  margin-top: 18rpx;
}
.field-label {
  display: block;
  margin-bottom: 10rpx;
  font-size: 24rpx;
  color: #7b8495;
}
.field-input,
.picker-value {
  width: 100%;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 18rpx;
  background: #ffffff;
  font-size: 26rpx;
  color: #1f2937;
  box-sizing: border-box;
  display: flex;
  align-items: center;
}
.picker-wrap {
  width: 100%;
}
.inline-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}
.inline-fields.single-column {
  grid-template-columns: 1fr;
}
.add-group-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 78rpx;
  margin-top: 18rpx;
  border-radius: 18rpx;
  border: 2rpx dashed #ffb37b;
  color: #ff7a1a;
  font-size: 26rpx;
}
.upload-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320rpx;
  border-radius: 24rpx;
  background: #fff7f0;
  overflow: hidden;
}
.preview-img {
  width: 100%;
  height: 320rpx;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.upload-title {
  margin-top: 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #ff7a1a;
}
.upload-desc {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}
.upload-mask {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.38);
}
.upload-mask-text {
  font-size: 22rpx;
  color: #ffffff;
}
.tips-title {
  display: block;
  margin-bottom: 18rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #202939;
}
.tips-item {
  display: block;
  font-size: 24rpx;
  line-height: 1.8;
  color: #7b8495;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(16rpx);
  box-shadow: 0 -10rpx 24rpx rgba(15, 23, 42, 0.05);
}
</style>
