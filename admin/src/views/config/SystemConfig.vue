<template>
  <div>
    <el-card>
      <el-form :model="configForm" label-width="140px" v-loading="loading" style="max-width: 500px">
        <el-form-item label="配送费(元)">
          <el-input-number v-model="configForm.delivery_fee" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="最低起送金额(元)">
          <el-input-number v-model="configForm.min_order_amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getConfigList, updateConfig } from '../../api/config'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const configForm = reactive({
  delivery_fee: 0,
  min_order_amount: 0
})

async function fetchConfig() {
  loading.value = true
  try {
    const res = await getConfigList()
    const data = res.data || {}
    configForm.delivery_fee = data.delivery_fee ?? 0
    configForm.min_order_amount = data.min_order_amount ?? 0
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await updateConfig(configForm)
  ElMessage.success('保存成功')
}

onMounted(fetchConfig)
</script>
