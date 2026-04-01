<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap">
        <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 220px" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="待支付" value="pending" />
          <el-option label="已支付" value="paid" />
          <el-option label="已接单" value="accepted" />
          <el-option label="已出餐" value="prepared" />
          <el-option label="配送中" value="delivering" />
          <el-option label="已送达" value="delivered" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="onDateChange" />
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="学生" width="120" />
        <el-table-column prop="merchantName" label="商家" width="140" />
        <el-table-column prop="riderName" label="骑手" width="110" />
        <el-table-column prop="totalPrice" label="金额">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="cancelableStatus.includes(row.status)" type="danger" text size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrderList, cancelOrder } from '../../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = {
  pending: '待支付',
  paid: '已支付',
  accepted: '已接单',
  prepared: '已出餐',
  delivering: '配送中',
  delivered: '已送达',
  completed: '已完成',
  cancelled: '已取消'
}
const statusType = {
  pending: 'warning',
  paid: 'warning',
  accepted: 'primary',
  prepared: 'primary',
  delivering: 'primary',
  delivered: 'success',
  completed: 'success',
  cancelled: 'info'
}
const cancelableStatus = ['pending', 'paid', 'accepted', 'prepared', 'delivering']

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({ status: '', orderNo: '', startDate: '', endDate: '', page: 1, size: 10 })

function onDateChange(val) {
  query.startDate = val ? val[0] : ''
  query.endDate = val ? val[1] : ''
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
  await cancelOrder(row.id)
  ElMessage.success('取消成功')
  fetchData()
}

onMounted(fetchData)
</script>
