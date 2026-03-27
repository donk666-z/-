<template>
  <u-tabbar
    :value="active"
    activeColor="#FF6B00"
    inactiveColor="#999999"
    :border="true"
    :placeholder="true"
    :safeAreaInsetBottom="true"
    :fixed="true"
    :zIndex="1000"
    @change="handleChange"
  >
    <u-tabbar-item name="home" text="首页" icon="home" />
    <u-tabbar-item name="order" text="订单" icon="order" />
    <u-tabbar-item name="user" text="我的" icon="account" />
  </u-tabbar>
</template>

<script>
export default {
  name: 'StudentTabBarOverlay',
  props: {
    active: {
      type: String,
      default: 'home'
    }
  },
  methods: {
    handleChange(name) {
      // 使用 redirectTo 避免在非 tab 配置下产生过多返回栈
      if (name === 'home') {
        uni.redirectTo({ url: '/pages/index/index' })
      } else if (name === 'order') {
        uni.redirectTo({ url: '/pages/order/list' })
      } else if (name === 'user') {
        uni.redirectTo({ url: '/pages/user/index' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
</style>

