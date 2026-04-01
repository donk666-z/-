<template>
  <div>
    <el-card>
      <template #header>平台基础配置</template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="configKey" label="配置键" width="220" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="配置值">
          <template #default="{ row }">
            <el-input v-model="row.configValue" placeholder="请输入配置值" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" text @click="saveRow(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getConfigList, updateConfig } from '../../api/config'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigList()
    list.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

async function saveRow(row) {
  await updateConfig(row.configKey, {
    configValue: row.configValue,
    description: row.description
  })
  ElMessage.success('配置已保存')
}

onMounted(fetchData)
</script>

