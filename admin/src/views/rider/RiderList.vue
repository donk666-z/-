<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap">
        <el-input
          v-model="query.keyword"
          placeholder="搜索骑手姓名 / 昵称 / 手机号"
          clearable
          style="width: 240px"
          @clear="fetchData"
          @keyup.enter="fetchData"
        />
        <el-select v-model="query.status" placeholder="接单状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="在线" value="online" />
          <el-option label="离线" value="offline" />
          <el-option label="配送中" value="delivering" />
        </el-select>
        <el-select v-model="query.accountStatus" placeholder="账号状态" clearable style="width: 140px" @change="fetchData">
          <el-option label="正常" value="active" />
          <el-option label="停用" value="inactive" />
          <el-option label="封禁" value="banned" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="骑手" width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="accountStatus" label="账号状态" width="120">
          <template #default="{ row }">
            <el-tag :type="accountStatusType[row.accountStatus] || 'info'">
              {{ accountStatusText[row.accountStatus] || row.accountStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="接单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">
              {{ statusText[row.status] || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="累计订单" width="100" />
        <el-table-column prop="totalIncome" label="累计收入" min-width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.totalIncome) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新" min-width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'online'"
              type="success"
              text
              size="small"
              @click="handleStatus(row, 'online')"
            >
              设为在线
            </el-button>
            <el-button
              v-if="row.status !== 'offline'"
              type="info"
              text
              size="small"
              @click="handleStatus(row, 'offline')"
            >
              设为离线
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        style="margin-top: 16px"
        type="info"
        :closable="false"
        title="账号封禁或解封仍在“用户管理”中统一处理，这里负责骑手资料展示与接单状态管理。"
      />

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRiderList, updateRiderStatus } from '../../api/rider'

const statusText = {
  online: '在线',
  offline: '离线',
  delivering: '配送中'
}

const statusType = {
  online: 'success',
  offline: 'info',
  delivering: 'warning'
}

const accountStatusText = {
  active: '正常',
  inactive: '停用',
  banned: '封禁'
}

const accountStatusType = {
  active: 'success',
  inactive: 'warning',
  banned: 'danger'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  keyword: '',
  status: '',
  accountStatus: '',
  page: 1,
  size: 10
})

function formatCurrency(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getRiderList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleStatus(row, status) {
  const label = statusText[status] || status
  await ElMessageBox.confirm(`确定将该骑手设为${label}吗？`, '提示', { type: 'warning' })
  await updateRiderStatus(row.id, status)
  ElMessage.success('状态更新成功')
  fetchData()
}

onMounted(fetchData)
</script>
