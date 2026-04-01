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
          <template #header>今日入账</template>
          <div class="stat-value">¥{{ stats.todayIncome ?? stats.todayRevenue ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>学生总数</template>
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
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>骑手总数</template>
          <div class="stat-value">{{ stats.totalRiders ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>进行中订单</template>
          <div class="stat-value">{{ stats.processingOrders ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>累计入账</template>
          <div class="stat-value">¥{{ stats.totalIncome ?? '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>待结算笔数</template>
          <div class="stat-value">{{ stats.pendingSettlementCount ?? '-' }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview } from '../../api/stats'

const stats = ref({})

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
