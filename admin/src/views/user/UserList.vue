<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px">
        <el-input v-model="query.keyword" placeholder="搜索昵称/手机号" clearable style="width: 240px" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="query.role" placeholder="角色筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="学生" value="student" />
          <el-option label="商户账号" value="merchant" />
          <el-option label="骑手" value="rider" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="正常" value="active" />
          <el-option label="停用" value="inactive" />
          <el-option label="封禁" value="banned" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag>{{ roleMap[row.role] || row.role || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ statusText[row.status] || row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'banned'" type="danger" text size="small" @click="toggleStatus(row, 'banned')">封禁</el-button>
            <el-button v-else type="success" text size="small" @click="toggleStatus(row, 'active')">解封</el-button>
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
import { getUserList, updateUserStatus } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const roleMap = {
  student: '学生',
  merchant: '商户账号',
  rider: '骑手',
  admin: '管理员'
}
const statusText = {
  active: '正常',
  inactive: '停用',
  banned: '封禁'
}
const statusType = {
  active: 'success',
  inactive: 'warning',
  banned: 'danger'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', role: '', status: '', page: 1, size: 10 })

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

async function toggleStatus(row, newStatus) {
  const action = newStatus === 'banned' ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定${action}该用户？`, '提示', { type: 'warning' })
  await updateUserStatus(row.id, newStatus)
  ElMessage.success(`${action}成功`)
  fetchData()
}

onMounted(fetchData)
</script>
