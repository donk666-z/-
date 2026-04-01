<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card shadow="never">总流水金额：¥{{ overview.totalAmount ?? 0 }}</el-card></el-col>
      <el-col :span="6"><el-card shadow="never">已完成金额：¥{{ overview.completedAmount ?? 0 }}</el-card></el-col>
      <el-col :span="6"><el-card shadow="never">待处理金额：¥{{ overview.pendingAmount ?? 0 }}</el-card></el-col>
      <el-col :span="6"><el-card shadow="never">今日流水：¥{{ overview.todayAmount ?? 0 }}</el-card></el-col>
    </el-row>

    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
        <el-input v-model="query.merchantId" placeholder="商户ID" style="width: 140px" clearable />
        <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
          <el-option label="收入" value="income" />
          <el-option label="提现" value="withdraw" />
          <el-option label="退款" value="refund" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="待处理" value="pending" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="流水ID" width="90" />
        <el-table-column prop="merchantName" label="商户" min-width="140" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ typeText[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ statusText[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'completed'"
              type="success"
              text
              size="small"
              @click="updateStatus(row, 'completed')"
            >标记完成</el-button>
            <el-button
              v-if="row.status !== 'failed'"
              type="danger"
              text
              size="small"
              @click="updateStatus(row, 'failed')"
            >标记失败</el-button>
            <el-button
              v-if="row.status !== 'pending'"
              type="warning"
              text
              size="small"
              @click="updateStatus(row, 'pending')"
            >设为待处理</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFinanceOverview, getTransactionList, updateTransactionStatus } from '../../api/finance'

const typeText = {
  income: '收入',
  withdraw: '提现',
  refund: '退款'
}
const statusText = {
  pending: '待处理',
  completed: '已完成',
  failed: '失败'
}
const statusType = {
  pending: 'warning',
  completed: 'success',
  failed: 'danger'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const overview = ref({})
const query = reactive({
  page: 1,
  size: 10,
  merchantId: '',
  type: '',
  status: ''
})

async function fetchOverview() {
  const res = await getFinanceOverview()
  overview.value = res.data || {}
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      ...query,
      merchantId: query.merchantId || undefined
    }
    const res = await getTransactionList(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function updateStatus(row, status) {
  await updateTransactionStatus(row.id, status)
  ElMessage.success('状态已更新')
  fetchOverview()
  fetchData()
}

onMounted(async () => {
  await fetchOverview()
  await fetchData()
})
</script>

