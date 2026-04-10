<template>
  <div>
    <el-card>
      <template #header>
        <div class="header-row">
          <span>系统配置</span>
          <span class="header-tip">关键平台规则优先展示，修改后会影响实际业务计算。</span>
        </div>
      </template>

      <el-table :data="displayList" v-loading="loading" stripe>
        <el-table-column label="配置项" min-width="180">
          <template #default="{ row }">
            <div class="config-title">{{ row.label || row.configKey }}</div>
            <div class="config-key">{{ row.configKey }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="240" />
        <el-table-column label="配置值" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.configValue" :placeholder="row.placeholder || '请输入配置值'" />
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfig } from '../../api/config'

const loading = ref(false)
const list = ref([])

const configMeta = {
  delivery_fee: {
    label: '配送费',
    description: '每笔订单默认配送费（元）',
    placeholder: '如 3.00'
  },
  min_order_amount: {
    label: '起送金额',
    description: '下单时最低商品金额（元）',
    placeholder: '如 10.00'
  },
  platform_commission: {
    label: '平台抽成',
    description: '平台抽成比例，范围为 0 到 1',
    placeholder: '如 0.15'
  }
}

const displayList = computed(() => {
  const map = {}
  list.value.forEach(item => {
    map[item.configKey] = item
  })

  const orderedKeys = ['delivery_fee', 'min_order_amount', 'platform_commission']
  const orderedList = orderedKeys.map(key => {
    const current = map[key] || { configKey: key, configValue: '', description: '' }
    return {
      ...current,
      label: configMeta[key].label,
      description: current.description || configMeta[key].description,
      placeholder: configMeta[key].placeholder
    }
  })

  const extraList = list.value
    .filter(item => !orderedKeys.includes(item.configKey))
    .map(item => ({ ...item, label: item.configKey }))

  return [...orderedList, ...extraList]
})

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
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-tip {
  color: #909399;
  font-size: 13px;
}

.config-title {
  color: #303133;
  font-weight: 600;
}

.config-key {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
</style>
