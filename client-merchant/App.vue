<script>
import merchantRealtime from '@/utils/merchant-realtime'

export default {
  onLaunch: function() {
    console.log('App Launch')
    this.syncRealtime()
  },
  onShow: function() {
    console.log('App Show')
    this.syncRealtime()
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    syncRealtime() {
      const token = uni.getStorageSync('token')
      if (!token) {
        merchantRealtime.stop()
        return
      }

      merchantRealtime.start({
        onNewOrder: (payload) => {
          const pendingCount = Number(payload.pendingCount || 0)
          if (this.$store) {
            this.$store.commit('SET_PENDING_COUNT', pendingCount)
          }
          uni.showToast({
            title: '收到新订单，请及时处理',
            icon: 'none'
          })
        }
      })
    }
  }
}
</script>

<style lang="scss">
@import '@/uni_modules/uview-ui/index.scss';

page {
  background-color: #f8f8f8;
}
</style>
