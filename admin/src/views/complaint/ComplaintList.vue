<template>
  <div>
    <el-card>
      <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
        <el-input v-model="query.keyword" placeholder="搜索投诉内容/联系方式" clearable style="width: 260px" @keyup.enter="fetchData" />
        <el-select v-model="query.type" placeholder="投诉类型" clearable style="width: 140px">
          <el-option label="商品问题" value="商品问题" />
          <el-option label="配送问题" value="配送问题" />
          <el-option label="服务态度" value="服务态度" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-select v-model="query.status" placeholder="处理状态" clearable style="width: 140px">
          <el-option label="待处理" value="pending" />
          <el-option label="已解决" value="resolved" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userName" label="提交人" width="120" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="content" label="投诉内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ statusText[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openReply(row)">处理</el-button>
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

    <el-dialog v-model="dialogVisible" title="投诉处理" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="投诉内容">
          <el-input type="textarea" :rows="4" :model-value="currentComplaint.content || ''" disabled />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已解决" value="resolved" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="form.reply" type="textarea" :rows="4" placeholder="请输入处理结果与说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getComplaintList, replyComplaint } from '../../api/complaint'

const statusText = {
  pending: '待处理',
  resolved: '已解决',
  closed: '已关闭'
}
const statusType = {
  pending: 'warning',
  resolved: 'success',
  closed: 'info'
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  status: '',
  type: '',
  keyword: ''
})

const dialogVisible = ref(false)
const currentComplaint = ref({})
const form = reactive({
  reply: '',
  status: 'resolved'
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getComplaintList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openReply(row) {
  currentComplaint.value = row
  form.reply = row.reply || ''
  form.status = row.status === 'closed' ? 'closed' : 'resolved'
  dialogVisible.value = true
}

async function submitReply() {
  await replyComplaint(currentComplaint.value.id, {
    reply: form.reply,
    status: form.status
  })
  ElMessage.success('处理完成')
  dialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

