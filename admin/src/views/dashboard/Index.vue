<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日订单</template>
          <div class="stat-value">{{ stats.todayOrders ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日收入</template>
          <div class="stat-value">¥{{ stats.todayIncome ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总用户数</template>
          <div class="stat-value">{{ stats.totalUsers ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总商家数</template>
          <div class="stat-value">{{ stats.totalMerchants ?? '-' }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card>
      <template #header>数据趋势</template>
      <div ref="chartRef" style="height: 360px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview } from '../../api/stats'

const stats = ref({})
const chartRef = ref(null)

onMounted(async () => {
  try {
    const res = await getOverview()
    stats.value = res.data
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.stat-value { font-size: 28px; font-weight: bold; color: #409eff; }
</style>
