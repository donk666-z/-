<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap">
        <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="待接单" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已取消" :value="3" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="onDateChange" />
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="用户" />
        <el-table-column prop="merchantName" label="商家" />
        <el-table-column prop="totalPrice" label="金额">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status < 2" type="danger" text size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
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

const statusMap = { 0: '待接单', 1: '进行中', 2: '已完成', 3: '已取消' }
const statusType = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({ status: '', startDate: '', endDate: '', page: 1, pageSize: 10 })

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
