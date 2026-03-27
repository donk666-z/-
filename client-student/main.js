import Vue from 'vue'
import App from './App'
import store from './store'
import uView from '@/uni_modules/uview-ui'

Vue.config.productionTip = false
Vue.use(uView)

App.mpType = 'app'

const app = new Vue({
  store,
  ...App
})
app.$mount()
