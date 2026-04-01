import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

import Layout from '../layout/Layout.vue'
import Login from '../views/login/Login.vue'
import Dashboard from '../views/dashboard/Index.vue'
import UserList from '../views/user/UserList.vue'
import OrderList from '../views/order/OrderList.vue'
import MerchantList from '../views/merchant/MerchantList.vue'
import CouponList from '../views/coupon/CouponList.vue'
import SystemConfig from '../views/config/SystemConfig.vue'
import FinanceList from '../views/finance/FinanceList.vue'
import ComplaintList from '../views/complaint/ComplaintList.vue'

const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'user', component: UserList },
      { path: 'order', component: OrderList },
      { path: 'merchant', component: MerchantList },
      { path: 'finance', component: FinanceList },
      { path: 'complaint', component: ComplaintList },
      { path: 'coupon', component: CouponList },
      { path: 'config', component: SystemConfig }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login') return next()
  if (!getToken()) return next('/login')
  next()
})

export default router
