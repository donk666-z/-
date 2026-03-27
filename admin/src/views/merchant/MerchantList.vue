<template>
  <div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="商家名称" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'">
              {{ row.status === 1 ? '正常' : row.status === 0 ? '待审核' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="monthSales" label="月销量" />
        <el-table-column prop="rating" label="评分" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" text size="small" @click="handleStatus(row, 1)">通过</el-button>
            <el-button v-if="row.status !== 2" type="danger" text size="small" @click="handleStatus(row, 2)">停用</el-button>
            <el-button v-if="row.status === 2" type="primary" text size="small" @click="handleStatus(row, 1)">启用</el-button>
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
import { getMerchantList, updateMerchantStatus } from '../../api/merchant'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })

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
  const label = status === 1 ? '通过/启用' : '停用'
  await ElMessageBox.confirm(`确定${label}该商家？`, '提示', { type: 'warning' })
  await updateMerchantStatus(row.id, status)
  ElMessage.success('操作成功')
  fetchData()
}

onMounted(fetchData)
</script>
