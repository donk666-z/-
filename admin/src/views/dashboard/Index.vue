<template>
  <div class="dashboard-page">
    <el-row :gutter="16" class="stats-grid">
      <el-col v-for="item in statCards" :key="item.key" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value" :class="{ money: item.type === 'currency' }">
            {{ formatStat(stats[item.key], item.type) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="trend-toolbar">
      <div class="toolbar-row">
        <div>
          <div class="section-title">趋势概览</div>
          <div class="section-subtitle">观察近一段时间的平台订单、收入与增长变化</div>
        </div>
        <el-radio-group v-model="days" size="default" @change="fetchTrend">
          <el-radio-button :label="7">近 7 天</el-radio-button>
          <el-radio-button :label="15">近 15 天</el-radio-button>
          <el-radio-button :label="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <el-row :gutter="16" class="chart-grid">
      <el-col :xs="24" :xl="12">
        <el-card shadow="never" class="chart-card" v-loading="trendLoading">
          <template #header>
            <div class="card-header">
              <span>订单量趋势</span>
              <span class="card-meta">{{ summaryText.order }}</span>
            </div>
          </template>
          <div ref="orderChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card shadow="never" class="chart-card" v-loading="trendLoading">
          <template #header>
            <div class="card-header">
              <span>平台收入趋势</span>
              <span class="card-meta">{{ summaryText.income }}</span>
            </div>
          </template>
          <div ref="incomeChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card growth-card" v-loading="trendLoading">
      <template #header>
        <div class="card-header">
          <span>平台增长趋势</span>
          <span class="card-meta">{{ summaryText.growth }}</span>
        </div>
      </template>
      <div ref="growthChartRef" class="chart chart-large"></div>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getOverview, getTrend } from '../../api/stats'

const stats = ref({})
const days = ref(7)
const trendLoading = ref(false)
const trend = ref({
  labels: [],
  orderTrend: [],
  incomeTrend: [],
  growthTrend: {
    users: [],
    merchants: [],
    riders: []
  }
})

const orderChartRef = ref(null)
const incomeChartRef = ref(null)
const growthChartRef = ref(null)

let orderChart
let incomeChart
let growthChart

const statCards = [
  { key: 'todayOrders', label: '今日订单', type: 'number' },
  { key: 'todayIncome', label: '今日收入', type: 'currency' },
  { key: 'totalUsers', label: '学生总数', type: 'number' },
  { key: 'totalMerchants', label: '商家总数', type: 'number' },
  { key: 'totalRiders', label: '骑手总数', type: 'number' },
  { key: 'processingOrders', label: '进行中订单', type: 'number' },
  { key: 'totalIncome', label: '累计收入', type: 'currency' },
  { key: 'pendingSettlementCount', label: '待结算笔数', type: 'number' }
]

const summaryText = computed(() => {
  const totalOrders = trend.value.orderTrend.reduce((sum, item) => sum + Number(item.value || 0), 0)
  const totalIncome = trend.value.incomeTrend.reduce((sum, item) => sum + Number(item.value || 0), 0)
  const totalUsers = trend.value.growthTrend.users.reduce((sum, item) => sum + Number(item.value || 0), 0)
  const totalMerchants = trend.value.growthTrend.merchants.reduce((sum, item) => sum + Number(item.value || 0), 0)
  const totalRiders = trend.value.growthTrend.riders.reduce((sum, item) => sum + Number(item.value || 0), 0)

  return {
    order: `${days.value} 天累计 ${totalOrders} 单`,
    income: `${days.value} 天累计 ${formatCurrency(totalIncome)}`,
    growth: `新增学生 ${totalUsers} / 商家 ${totalMerchants} / 骑手 ${totalRiders}`
  }
})

function formatCurrency(value) {
  const amount = Number(value || 0)
  return `¥${amount.toFixed(2)}`
}

function formatStat(value, type) {
  if (type === 'currency') {
    return formatCurrency(value)
  }
  return value ?? 0
}

async function fetchOverview() {
  const res = await getOverview()
  stats.value = res.data || {}
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    const res = await getTrend(days.value)
    trend.value = {
      labels: res.data?.labels || [],
      orderTrend: res.data?.orderTrend || [],
      incomeTrend: res.data?.incomeTrend || [],
      growthTrend: {
        users: res.data?.growthTrend?.users || [],
        merchants: res.data?.growthTrend?.merchants || [],
        riders: res.data?.growthTrend?.riders || []
      }
    }
    await nextTick()
    renderCharts()
  } finally {
    trendLoading.value = false
  }
}

function ensureChart(instance, el) {
  if (!el) {
    return instance
  }
  if (!instance) {
    return echarts.init(el)
  }
  return instance
}

function renderCharts() {
  orderChart = ensureChart(orderChart, orderChartRef.value)
  incomeChart = ensureChart(incomeChart, incomeChartRef.value)
  growthChart = ensureChart(growthChart, growthChartRef.value)

  const labels = trend.value.labels

  orderChart?.setOption({
    color: ['#409eff'],
    grid: { left: 36, right: 20, top: 32, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.12)'
        },
        data: trend.value.orderTrend.map(item => Number(item.value || 0))
      }
    ]
  })

  incomeChart?.setOption({
    color: ['#67c23a'],
    grid: { left: 48, right: 20, top: 32, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      valueFormatter: value => formatCurrency(value)
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '平台收入',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: {
          color: 'rgba(103, 194, 58, 0.12)'
        },
        data: trend.value.incomeTrend.map(item => Number(item.value || 0))
      }
    ]
  })

  growthChart?.setOption({
    color: ['#e6a23c', '#f56c6c', '#909399'],
    grid: { left: 36, right: 20, top: 32, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '新增学生',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.value.growthTrend.users.map(item => Number(item.value || 0))
      },
      {
        name: '新增商家',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.value.growthTrend.merchants.map(item => Number(item.value || 0))
      },
      {
        name: '新增骑手',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trend.value.growthTrend.riders.map(item => Number(item.value || 0))
      }
    ]
  })
}

function handleResize() {
  orderChart?.resize()
  incomeChart?.resize()
  growthChart?.resize()
}

onMounted(async () => {
  await fetchOverview()
  await fetchTrend()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  orderChart?.dispose()
  incomeChart?.dispose()
  growthChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid,
.chart-grid {
  margin: 0;
}

.stat-card {
  border-radius: 8px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 30px;
  line-height: 1.1;
  font-weight: 700;
  color: #303133;
}

.stat-value.money {
  color: #409eff;
}

.trend-toolbar {
  border-radius: 8px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.chart-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.card-meta {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.chart {
  width: 100%;
  height: 320px;
}

.chart-large {
  height: 360px;
}

.growth-card {
  margin-bottom: 8px;
}

@media (max-width: 1200px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
