<template>
  <div>
    <el-card>
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="活动名称" clearable style="width: 220px" @keyup.enter="fetchData" />
          <el-select v-model="query.status" placeholder="活动状态" clearable style="width: 140px" @change="fetchData">
            <el-option label="草稿" value="draft" />
            <el-option label="进行中" value="active" />
            <el-option label="已停用" value="inactive" />
          </el-select>
          <el-button type="primary" @click="fetchData">搜索</el-button>
        </div>
        <el-button type="primary" @click="openDialog(null)">新增活动</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column prop="couponName" label="关联优惠券" min-width="160" />
        <el-table-column label="优惠力度" width="120">
          <template #default="{ row }">
            {{ formatCouponText(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="claimLimitPerUser" label="每人限领" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] || 'info'">{{ statusText[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.status !== 'active'" type="success" text size="small" @click="changeStatus(row, 'active')">启用</el-button>
            <el-button v-if="row.status !== 'inactive'" type="warning" text size="small" @click="changeStatus(row, 'inactive')">停用</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑促销活动' : '新增促销活动'" width="620px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="活动名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="关联优惠券">
          <el-select v-model="form.couponId" style="width: 100%" filterable>
            <el-option v-for="item in couponOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.claimLimitPerUser" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="draft" />
            <el-option label="进行中" value="active" />
            <el-option label="已停用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCouponList } from '../../api/coupon'
import { createPromotion, getPromotionList, updatePromotion, updatePromotionStatus } from '../../api/promotion'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const couponOptions = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)

const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10
})

const defaultForm = {
  name: '',
  couponId: null,
  claimLimitPerUser: 1,
  startTime: '',
  endTime: '',
  status: 'draft',
  description: ''
}

const form = reactive({ ...defaultForm })

const statusText = {
  draft: '草稿',
  active: '进行中',
  inactive: '已停用'
}

const statusType = {
  draft: 'info',
  active: 'success',
  inactive: 'warning'
}

function formatCouponText(row) {
  if (row.couponType === 'discount') {
    return `${row.discount} 折`
  }
  return `减 ${row.discount}`
}

async function fetchCouponOptions() {
  const res = await getCouponList({ page: 1, size: 1000 })
  couponOptions.value = res.data.records || []
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPromotionList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      ...defaultForm,
      ...row
    })
  } else {
    editingId.value = null
    Object.assign(form, defaultForm)
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const payload = { ...form }
  if (editingId.value) {
    await updatePromotion(editingId.value, payload)
  } else {
    await createPromotion(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

async function changeStatus(row, status) {
  const label = statusText[status] || status
  await ElMessageBox.confirm(`确定将该活动设为${label}吗？`, '提示', { type: 'warning' })
  await updatePromotionStatus(row.id, status)
  ElMessage.success('状态更新成功')
  fetchData()
}

onMounted(() => {
  fetchCouponOptions()
  fetchData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 12px;
}
</style>
