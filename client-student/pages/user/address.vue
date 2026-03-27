<template>
  <view class="container">
    <!-- 地址列表 -->
    <view class="address-list">
      <view
        class="address-item"
        v-for="addr in addresses"
        :key="addr.id"
        @click="handleSelect(addr)"
      >
        <view class="address-main">
          <view class="name-phone">
            <text class="name">{{ addr.name }}</text>
            <text class="phone">{{ addr.phone }}</text>
            <text class="default-tag" v-if="addr.isDefault">默认</text>
          </view>
          <view class="detail">{{ addr.address }} {{ addr.building }} {{ addr.room }}</view>
        </view>
        <view class="address-actions" @click.stop>
          <u-icon name="edit-pen" size="20" color="#666" @click="editAddress(addr)"></u-icon>
          <u-icon name="trash" size="20" color="#ff4d4f" @click="removeAddress(addr.id)"></u-icon>
        </view>
      </view>
      <view class="empty-tip" v-if="!addresses.length">
        <u-icon name="map" size="60" color="#ccc"></u-icon>
        <text>暂无收货地址</text>
      </view>
    </view>

    <!-- 新增按钮 -->
    <view class="add-btn-wrap">
      <button class="add-btn" @click="openForm()">+ 新增地址</button>
    </view>

    <!-- 表单弹窗 -->
    <u-popup :show="showForm" mode="bottom" round="20" @close="closeForm">
      <view class="form-popup">
        <view class="form-header">
          <text class="form-title">{{ editingId ? '编辑地址' : '新增地址' }}</text>
          <u-icon name="close" size="22" color="#999" @click="closeForm"></u-icon>
        </view>

        <view class="form-body">
          <view class="form-item">
            <text class="label">收货人</text>
            <input class="input" v-model="form.name" placeholder="请输入收货人姓名" maxlength="20" />
          </view>
          <view class="form-item">
            <text class="label">手机号</text>
            <input class="input" v-model="form.phone" placeholder="请输入手机号" type="number" maxlength="11" />
          </view>
          <view class="form-item">
            <text class="label">地址</text>
            <input class="input" v-model="form.address" placeholder="如：XX大学XX校区" maxlength="100" />
          </view>
          <view class="form-item">
            <text class="label">楼栋</text>
            <input class="input" v-model="form.building" placeholder="如：3号宿舍楼" maxlength="50" />
          </view>
          <view class="form-item">
            <text class="label">房间号</text>
            <input class="input" v-model="form.room" placeholder="如：305" maxlength="20" />
          </view>
          <view class="form-item switch-item">
            <text class="label">设为默认地址</text>
            <switch :checked="form.isDefault" @change="form.isDefault = $event.detail.value" color="#ff6b00" />
          </view>
        </view>

        <button class="save-btn" :loading="saving" @click="saveAddress">保存</button>
      </view>
    </u-popup>

    <StudentTabBarOverlay active="user" />
  </view>
</template>

<script>
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/user'
import StudentTabBarOverlay from '@/components/StudentTabBarOverlay.vue'

export default {
  components: { StudentTabBarOverlay },
  data() {
    return {
      addresses: [],
      showForm: false,
      editingId: null,
      saving: false,
      selectMode: false,
      form: {
        name: '',
        phone: '',
        address: '',
        building: '',
        room: '',
        isDefault: false
      }
    }
  },
  onLoad(options) {
    this.selectMode = options.mode === 'select'
    this.loadAddresses()
  },
  methods: {
    async loadAddresses() {
      try {
        this.addresses = await getAddresses()
      } catch (e) {
        console.error(e)
      }
    },

    handleSelect(addr) {
      if (this.selectMode) {
        // 选择模式：把地址传回上一页
        const eventChannel = this.getOpenerEventChannel()
        if (eventChannel) {
          eventChannel.emit('selectAddress', addr)
        }
        uni.navigateBack()
      }
    },

    openForm(addr) {
      if (addr) {
        this.editingId = addr.id
        this.form = {
          name: addr.name,
          phone: addr.phone,
          address: addr.address,
          building: addr.building,
          room: addr.room,
          isDefault: !!addr.isDefault
        }
      } else {
        this.editingId = null
        this.form = { name: '', phone: '', address: '', building: '', room: '', isDefault: false }
      }
      this.showForm = true
    },

    closeForm() {
      this.showForm = false
      this.editingId = null
    },

    editAddress(addr) {
      this.openForm(addr)
    },

    validateForm() {
      const { name, phone, address, building, room } = this.form
      if (!name.trim()) {
        uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
        return false
      }
      if (!phone.trim()) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(phone.trim())) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return false
      }
      if (!address.trim()) {
        uni.showToast({ title: '请输入地址', icon: 'none' })
        return false
      }
      if (!building.trim()) {
        uni.showToast({ title: '请输入楼栋', icon: 'none' })
        return false
      }
      if (!room.trim()) {
        uni.showToast({ title: '请输入房间号', icon: 'none' })
        return false
      }
      return true
    },

    async saveAddress() {
      if (!this.validateForm()) return
      this.saving = true
      try {
        const data = {
          name: this.form.name.trim(),
          phone: this.form.phone.trim(),
          address: this.form.address.trim(),
          building: this.form.building.trim(),
          room: this.form.room.trim(),
          isDefault: this.form.isDefault
        }
        if (this.editingId) {
          await updateAddress(this.editingId, data)
        } else {
          await addAddress(data)
        }
        uni.showToast({ title: '保存成功', icon: 'success' })
        this.closeForm()
        this.loadAddresses()
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
      }
    },

    removeAddress(id) {
      uni.showModal({
        title: '提示',
        content: '确定删除该地址？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteAddress(id)
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.loadAddresses()
            } catch (e) {
              console.error(e)
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  background: #f8f8f8;
  min-height: 100vh;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.address-list {
  .address-item {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;

    .address-main {
      flex: 1;

      .name-phone {
        display: flex;
        align-items: center;
        margin-bottom: 12rpx;

        .name {
          font-size: 30rpx;
          font-weight: bold;
          margin-right: 16rpx;
        }

        .phone {
          font-size: 26rpx;
          color: #666;
          margin-right: 16rpx;
        }

        .default-tag {
          font-size: 20rpx;
          color: #ff6b00;
          border: 1rpx solid #ff6b00;
          padding: 2rpx 12rpx;
          border-radius: 6rpx;
        }
      }

      .detail {
        font-size: 26rpx;
        color: #999;
        line-height: 1.4;
      }
    }

    .address-actions {
      display: flex;
      align-items: center;
      gap: 24rpx;
      margin-left: 20rpx;
    }
  }

  .empty-tip {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 0;
    gap: 20rpx;

    text {
      font-size: 28rpx;
      color: #999;
    }
  }
}

.add-btn-wrap {
  position: fixed;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  z-index: 1101;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);

  .add-btn {
    background: #ff6b00;
    color: #fff;
    border-radius: 50rpx;
    font-size: 30rpx;
    height: 88rpx;
    line-height: 88rpx;
  }
}

.form-popup {
  padding: 30rpx;
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));

  .form-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;

    .form-title {
      font-size: 32rpx;
      font-weight: bold;
    }
  }

  .form-body {
    .form-item {
      display: flex;
      align-items: center;
      padding: 24rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      .label {
        width: 160rpx;
        font-size: 28rpx;
        color: #333;
        flex-shrink: 0;
      }

      .input {
        flex: 1;
        font-size: 28rpx;
      }

      &.switch-item {
        justify-content: space-between;
        border-bottom: none;
        padding: 30rpx 0;
      }
    }
  }

  .save-btn {
    margin-top: 40rpx;
    background: #ff6b00;
    color: #fff;
    border-radius: 50rpx;
    font-size: 30rpx;
    height: 88rpx;
    line-height: 88rpx;
  }
}
</style>
