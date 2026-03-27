<template>
  <div>
    <el-card>
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openDialog(null)">新增优惠券</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型">
          <template #default="{ row }">{{ row.type === 1 ? '满减' : '折扣' }}</template>
        </el-table-column>
        <el-table-column prop="discount" label="优惠值" />
        <el-table-column prop="minAmount" label="最低消费">
          <template #default="{ row }">¥{{ row.minAmount }}</template>
        </el-table-column>
        <el-table-column prop="total" label="总量" />
        <el-table-column prop="claimed" label="已领取" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新增优惠券'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="满减" :value="1" />
            <el-option label="折扣" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠值">
          <el-input-number v-model="form.discount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低消费">
          <el-input-number v-model="form.minAmount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.total" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { ref, reactive, onMounted } from 'vue'
import { getCouponList, createCoupon, updateCoupon, deleteCoupon } from '../../api/coupon'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })
const dialogVisible = ref(false)
const editingId = ref(null)
const defaultForm = { name: '', type: 1, discount: 0, minAmount: 0, total: 0, status: 1 }
const form = reactive({ ...defaultForm })

async function fetchData() {
  loading.value = true
  try {
    const res = await getCouponList(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
  } else {
    editingId.value = null
    Object.assign(form, defaultForm)
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (editingId.value) {
    await updateCoupon(editingId.value, form)
  } else {
    await createCoupon(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该优惠券？', '提示', { type: 'warning' })
  await deleteCoupon(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

