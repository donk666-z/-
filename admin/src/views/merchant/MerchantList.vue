<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px">
        <el-input v-model="query.keyword" placeholder="商家名称" clearable style="width: 220px" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="营业中" value="open" />
          <el-option label="打烊" value="closed" />
          <el-option label="繁忙" value="busy" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="商家名称" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">
              {{ statusText[row.status] || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="monthSales" label="月销量" />
        <el-table-column prop="rating" label="评分" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'open'" type="success" text size="small" @click="handleStatus(row, 'open')">设为营业</el-button>
            <el-button v-if="row.status !== 'busy'" type="warning" text size="small" @click="handleStatus(row, 'busy')">设为繁忙</el-button>
            <el-button v-if="row.status !== 'closed'" type="danger" text size="small" @click="handleStatus(row, 'closed')">设为打烊</el-button>
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
import { getMerchantList, updateMerchantStatus } from '../../api/merchant'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusText = {
  open: '营业中',
  closed: '打烊',
  busy: '繁忙'
}
const statusType = {
  open: 'success',
  closed: 'info',
  busy: 'warning'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await getMerchantList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleStatus(row, status) {
  const label = statusText[status] || status
  await ElMessageBox.confirm(`确定${label}该商家？`, '提示', { type: 'warning' })
  await updateMerchantStatus(row.id, status)
  ElMessage.success('操作成功')
  fetchData()
}

onMounted(fetchData)
</script>
