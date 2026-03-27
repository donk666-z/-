<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px">
        <el-input v-model="query.keyword" placeholder="搜索昵称/手机号" clearable style="width: 240px" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="query.role" placeholder="角色筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="普通用户" value="user" />
          <el-option label="骑手" value="rider" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag>{{ row.role === 'rider' ? '骑手' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="danger" text size="small" @click="toggleStatus(row)">封禁</el-button>
            <el-button v-else type="success" text size="small" @click="toggleStatus(row)">解封</el-button>
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
import { getUserList, updateUserStatus } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', role: '', page: 1, pageSize: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定${action}该用户？`, '提示', { type: 'warning' })
  await updateUserStatus(row.id, newStatus)
  ElMessage.success(`${action}成功`)
  fetchData()
}

onMounted(fetchData)
</script>
