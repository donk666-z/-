<template>
  <view class="container">
    <view class="hero-card">
      <text class="hero-title">{{ isEdit ? '编辑菜品' : '新增菜品' }}</text>
      <text class="hero-subtitle">
        {{ form.type === 'combo' ? '套餐可以配置多个可选分组，库存会根据套餐内单品动态计算' : '单品会直接作为可售卖商品展示给学生' }}
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
        <view v-else class="combo-tip">套餐库存会根据所选单品库存自动计算，无需手动填写。</view>
        <u-form-item label="菜品描述" prop="description">
          <u-input
            v-model="form.description"
            type="textarea"
            autoHeight
            placeholder="简单介绍一下卖点、口味或适合人群"
          ></u-input>
        </u-form-item>
      </u-form>
    </view>

    <view class="form-card" v-if="form.type === 'combo'">
      <view class="section-header">
        <text class="section-title">套餐配置</text>
        <text class="section-tip">套餐选项只能引用单品</text>
      </view>

      <view v-if="selectableDishes.length === 0" class="empty-config">
        请先创建单品，再回来配置套餐选项。
      </view>

      <view v-for="(group, groupIndex) in form.comboConfig.groups" :key="groupIndex" class="combo-group">
        <view class="group-header">
          <text class="group-title">分组 {{ groupIndex + 1 }}</text>
          <text class="group-remove" @click="removeComboGroup(groupIndex)">删除分组</text>
        </view>

        <view class="field-block">
          <text class="field-label">分组名称</text>
          <input v-model="group.name" class="field-input" placeholder="例如：主食、饮料、小食" />
        </view>

        <view class="inline-fields">
          <view class="inline-field">
            <text class="field-label">最少选择</text>
            <input v-model="group.minSelect" class="field-input" type="number" />
          </view>
          <view class="inline-field">
            <text class="field-label">最多选择</text>
            <input v-model="group.maxSelect" class="field-input" type="number" />
          </view>
        </view>

        <view v-for="(option, optionIndex) in group.options" :key="optionIndex" class="option-card">
          <view class="option-header">
            <text class="option-title">选项 {{ optionIndex + 1 }}</text>
            <text class="group-remove" @click="removeComboOption(groupIndex, optionIndex)">删除选项</text>
          </view>

          <view class="field-block">
            <text class="field-label">选择单品</text>
            <picker
              class="picker-wrap"
              :range="selectableDishes"
              range-key="name"
              :value="getDishPickerIndex(option.dishId)"
              @change="onSelectOptionDish(groupIndex, optionIndex, $event)"
            >
              <view class="picker-value">{{ getDishLabel(option.dishId) || '请选择单品' }}</view>
            </picker>
          </view>

          <view class="inline-fields">
            <view class="inline-field">
              <text class="field-label">数量</text>
              <input v-model="option.quantity" class="field-input" type="number" />
            </view>
            <view class="inline-field">
              <text class="field-label">加价</text>
              <input v-model="option.extraPrice" class="field-input" type="digit" />
            </view>
          </view>
        </view>

        <view class="add-link" @click="addComboOption(groupIndex)">+ 添加选项</view>
      </view>

      <view class="add-group-btn" @click="addComboGroup">+ 添加套餐分组</view>
    </view>

    <view class="form-card">
      <view class="section-header">
        <text class="section-title">菜品图片</text>
        <text class="section-tip">建议上传清晰正方形图片</text>
      </view>

      <view class="upload-panel" @click="chooseImage">
        <image
          v-if="form.image"
          :src="form.image"
          class="preview-img"
          mode="aspectFill"
        ></image>
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
      <text class="tips-item">1. 套餐价格建议填写套餐基础价，选项加价会在下单时自动累加。</text>
      <text class="tips-item">2. 套餐内每个分组都可以配置最少/最多选择数量，学生端会按这里限制选择。</text>
      <text class="tips-item">3. 套餐库存由选项单品库存共同决定，某些单品下架后对应选项会自动不可选。</text>
    </view>

    <view class="bottom-bar">
      <u-button type="primary" shape="circle" @click="handleSubmit" :loading="loading">
        {{ isEdit ? '保存修改' : '确认新增' }}
      </u-button>
    </view>
  </view>
</template>

<script>
import { getDishDetail, createDish, updateDish, getDishList } from '@/api/dish'
import { UPLOAD_URL } from '@/config/api'

const emptyOption = () => ({
  dishId: null,
  quantity: '1',
  extraPrice: '0'
})

const emptyGroup = () => ({
  name: '',
  minSelect: '1',
  maxSelect: '1',
  options: [emptyOption()]
})

export default {
  data() {
    return {
      isEdit: false,
      dishId: '',
      loading: false,
      selectableDishes: [],
      form: {
        type: 'single',
        name: '',
        description: '',
        price: '',
        stock: '',
        category: '',
        image: '',
        comboConfig: {
          groups: []
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
      if (this.isEdit) {
        await this.loadDish()
      }
      await this.loadSelectableDishes()
      if (this.form.type === 'combo' && this.form.comboConfig.groups.length === 0) {
        this.addComboGroup()
      }
    },
    async loadSelectableDishes() {
      try {
        const response = await getDishList()
        const list = (response.records || response || []).map((dish) => ({
          id: Number(dish.id),
          name: dish.name || '',
          type: this.normalizeDishType(dish.type)
        }))
        this.selectableDishes = list.filter((dish) => dish.type === 'single' && String(dish.id) !== String(this.dishId))
      } catch (error) {
        console.error('加载可选单品失败', error)
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
            groups: this.normalizeComboGroups(dish.comboConfig && dish.comboConfig.groups)
          }
        }
      } catch (error) {
        console.error('加载菜品详情失败', error)
      }
    },
    normalizeComboGroups(groups) {
      if (!Array.isArray(groups)) {
        return []
      }
      return groups.map((group) => ({
        name: group.name || '',
        minSelect: String(group.minSelect !== undefined && group.minSelect !== null ? group.minSelect : 1),
        maxSelect: String(group.maxSelect !== undefined && group.maxSelect !== null ? group.maxSelect : 1),
        options: Array.isArray(group.options) && group.options.length
          ? group.options.map((option) => ({
              dishId: option.dishId || null,
              quantity: String(option.quantity || 1),
              extraPrice: String(option.extraPrice || 0)
            }))
          : [emptyOption()]
      }))
    },
    setDishType(type) {
      this.form.type = type
      if (type === 'combo' && this.form.comboConfig.groups.length === 0) {
        this.addComboGroup()
      }
    },
    addComboGroup() {
      this.form.comboConfig.groups.push(emptyGroup())
    },
    removeComboGroup(groupIndex) {
      this.form.comboConfig.groups.splice(groupIndex, 1)
      if (this.form.comboConfig.groups.length === 0 && this.form.type === 'combo') {
        this.form.comboConfig.groups.push(emptyGroup())
      }
    },
    addComboOption(groupIndex) {
      this.form.comboConfig.groups[groupIndex].options.push(emptyOption())
    },
    removeComboOption(groupIndex, optionIndex) {
      const options = this.form.comboConfig.groups[groupIndex].options
      options.splice(optionIndex, 1)
      if (options.length === 0) {
        options.push(emptyOption())
      }
    },
    getDishPickerIndex(dishId) {
      const index = this.selectableDishes.findIndex((dish) => String(dish.id) === String(dishId))
      return index >= 0 ? index : 0
    },
    getDishLabel(dishId) {
      const matchedDish = this.selectableDishes.find((dish) => String(dish.id) === String(dishId))
      return matchedDish ? matchedDish.name : ''
    },
    onSelectOptionDish(groupIndex, optionIndex, event) {
      const pickedDish = this.selectableDishes[Number(event.detail.value)]
      if (!pickedDish) {
        return
      }
      this.form.comboConfig.groups[groupIndex].options[optionIndex].dishId = pickedDish.id
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
              const data = JSON.parse(uploadRes.data)
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
      const groups = this.form.comboConfig.groups
      if (!Array.isArray(groups) || groups.length === 0) {
        throw new Error('请至少配置一个套餐分组')
      }
      if (this.selectableDishes.length === 0) {
        throw new Error('请先创建单品，再配置套餐')
      }

      return {
        groups: groups.map((group, groupIndex) => {
          const name = (group.name || '').trim()
          const minSelect = parseInt(group.minSelect, 10)
          const maxSelect = parseInt(group.maxSelect, 10)

          if (!name) {
            throw new Error(`请填写第 ${groupIndex + 1} 个分组名称`)
          }
          if (!Number.isInteger(minSelect) || minSelect < 0) {
            throw new Error(`第 ${groupIndex + 1} 个分组的最少选择数量不合法`)
          }
          if (!Number.isInteger(maxSelect) || maxSelect <= 0 || maxSelect < minSelect) {
            throw new Error(`第 ${groupIndex + 1} 个分组的最多选择数量不合法`)
          }

          const options = Array.isArray(group.options) ? group.options : []
          if (!options.length) {
            throw new Error(`第 ${groupIndex + 1} 个分组至少需要一个选项`)
          }

          const seenDishIds = new Set()
          return {
            name,
            minSelect,
            maxSelect,
            options: options.map((option, optionIndex) => {
              const dishId = Number(option.dishId)
              const quantity = parseInt(option.quantity, 10)
              const extraPrice = Number(option.extraPrice || 0)

              if (!Number.isFinite(dishId)) {
                throw new Error(`第 ${groupIndex + 1} 个分组选项 ${optionIndex + 1} 还没有选择单品`)
              }
              if (seenDishIds.has(dishId)) {
                throw new Error(`第 ${groupIndex + 1} 个分组存在重复单品`)
              }
              seenDishIds.add(dishId)

              if (!Number.isInteger(quantity) || quantity <= 0) {
                throw new Error(`第 ${groupIndex + 1} 个分组选项 ${optionIndex + 1} 的数量不合法`)
              }
              if (!Number.isFinite(extraPrice) || extraPrice < 0) {
                throw new Error(`第 ${groupIndex + 1} 个分组选项 ${optionIndex + 1} 的加价不合法`)
              }

              return {
                dishId,
                quantity,
                extraPrice
              }
            })
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

.group-header,
.option-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-title,
.option-title {
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

.picker-value {
  color: #1f2937;
}

.inline-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}

.option-card {
  margin-top: 18rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.9);
}

.add-link,
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
