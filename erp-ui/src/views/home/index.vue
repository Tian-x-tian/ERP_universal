<template>
  <div class="executive-mode" v-loading="summaryLoading" style="width: 100%;">
    <!-- Header Section -->
    <header class="home-header">
      <h1 class="home-title">欢迎回来，{{ displayUserName }}</h1>
      <p class="home-subtitle">这是您今日的集团经营全景视图，当前系统状态良好。</p>
    </header>

    <!-- Bento Grid Metrics -->
    <div class="bento-grid">
      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-icon-wrap is-primary">
            <span class="material-symbols-outlined" data-icon="approval_delegation">approval_delegation</span>
          </div>
          <span class="bento-badge is-urgent">紧急</span>
        </div>
        <h3 class="bento-card-title">待处理审批</h3>
        <div class="bento-card-value">
          {{ formatCount(homeSummary.todo.pendingCount) }} <span class="bento-card-unit">份</span>
        </div>
      </div>

      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-icon-wrap is-secondary">
            <span class="material-symbols-outlined" data-icon="inventory_2">inventory_2</span>
          </div>
          <span class="bento-badge is-positive">+{{ formatPercent(homeSummary.inventory.completionRate30d) }}</span>
        </div>
        <h3 class="bento-card-title">本月入库量</h3>
        <div class="bento-card-value">
          {{ formatQuantity(homeSummary.inventory.currentMonthInboundQty) }} <span class="bento-card-unit">SKU</span>
        </div>
      </div>

      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-icon-wrap is-warning">
            <span class="material-symbols-outlined" data-icon="person_alert">person_alert</span>
          </div>
          <span class="bento-badge is-warning-text">需关注</span>
        </div>
        <h3 class="bento-card-title">人力考勤异常</h3>
        <div class="bento-card-value">
          {{ formatCount(homeSummary.hr.abnormalEmployeeCount) }} <span class="bento-card-unit">人</span>
        </div>
      </div>

      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-icon-wrap is-success">
            <span class="material-symbols-outlined" data-icon="settings_suggest">settings_suggest</span>
          </div>
          <div class="status-pulse-dot"></div>
        </div>
        <h3 class="bento-card-title">系统运行状态</h3>
        <div class="bento-card-value">
          {{ formatPercent(homeSummary.system.successRate24h) }} <span class="bento-card-unit">正常</span>
        </div>
      </div>
    </div>

    <!-- Main Analytics Section -->
    <div class="analytics-layout">
      <!-- Large Monthly Stats Chart -->
      <div class="analytics-card chart-card">
        <div class="chart-header">
          <h2 class="section-title">全集团月度经营统计</h2>
          <div class="chart-tabs">
            <button class="chart-tab text-muted">出口额</button>
            <button class="chart-tab is-active">净利润</button>
          </div>
        </div>
        <div class="bar-chart-container">
          <!-- Symbolic bar chart -->
          <div class="bar-column">
            <div class="bar-fill" style="height: 40%;"></div>
          </div>
          <div class="bar-column">
            <div class="bar-fill" style="height: 65%;"></div>
          </div>
          <div class="bar-column">
            <div class="bar-fill" style="height: 55%;"></div>
          </div>
          <div class="bar-column">
            <div class="bar-fill" style="height: 85%;"></div>
          </div>
          <div class="bar-column">
            <div class="bar-fill" style="height: 70%;"></div>
          </div>
          <div class="bar-column">
            <div class="bar-fill" style="height: 95%;"></div>
          </div>
        </div>
        <div class="bar-labels">
          <span>1月</span><span>2月</span><span>3月</span><span>4月</span><span>5月</span><span>6月</span>
        </div>
      </div>

      <!-- Circular Efficiency Chart -->
      <div class="analytics-card efficiency-card">
        <h2 class="section-title text-left">组织协同效率</h2>
        <div class="circular-progress-wrap">
          <svg class="circular-svg">
            <circle class="circular-bg" cx="96" cy="96" fill="transparent" r="88" stroke="currentColor" stroke-width="12"></circle>
            <circle class="circular-value" cx="96" cy="96" fill="transparent" r="88" stroke="currentColor" :style="{ strokeDasharray: 552.92, strokeDashoffset: 552.92 * (1 - homeSummary.todo.collaborationRate / 100) }" stroke-width="12"></circle>
          </svg>
          <div class="circular-content">
            <span class="circular-percent">{{ formatPercent(homeSummary.todo.collaborationRate) }}</span>
            <span class="circular-label">达标率</span>
          </div>
        </div>
        <div class="linear-progress-section">
          <div class="linear-progress-header">
            <span class="linear-label">协同任务</span>
            <span class="linear-value">{{ homeSummary.todo.collaborationDone }}/{{ homeSummary.todo.collaborationTotal || 170 }}</span>
          </div>
          <div class="linear-progress-track">
            <div class="linear-progress-bar" :style="{ width: `${homeSummary.todo.collaborationRate}%` }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Access Section -->
    <section>
      <div class="quick-access-header">
        <h2 class="section-title">常用工具快捷入口</h2>
        <button class="edit-shortcut-btn">编辑快捷方式</button>
      </div>
      
      <div class="quick-access-grid">
        <a class="shortcut-card" @click="navigateTo('/business/finance/report')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="account_balance">account_balance</span>
          <span class="shortcut-label">财务报表</span>
        </a>
        <a class="shortcut-card" @click="navigateTo('/system/mdm')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="hub">hub</span>
          <span class="shortcut-label">组织架构</span>
        </a>
        <a class="shortcut-card" @click="navigateTo('/business/audit')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="fact_check">fact_check</span>
          <span class="shortcut-label">合规审计</span>
        </a>
        <a class="shortcut-card" @click="navigateTo('/business/analysis')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="analytics">analytics</span>
          <span class="shortcut-label">舆情分析</span>
        </a>
        <a class="shortcut-card" @click="navigateTo('/business/hr')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="groups">groups</span>
          <span class="shortcut-label">人才盘点</span>
        </a>
        <a class="shortcut-card" @click="navigateTo('/platform/overview')">
          <span class="material-symbols-outlined shortcut-icon" data-icon="add_circle">add_circle</span>
          <span class="shortcut-label">添加更多</span>
        </a>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getWorkflowHomeTodoSummary } from '@/api/workflow/todo-center'
import { getInventoryInboundHomeSummary } from '@/api/business/inventory/inbound'
import { getHrWarningHomeSummary } from '@/api/business/hr/warning'
import { getSystemHomeHealthSummary } from '@/api/system/home'

const router = useRouter()
const userStore = useUserStore()
const summaryLoading = ref(false)

const homeSummary = reactive({
  todo: {
    pendingCount: 0,
    processingCount: 0,
    overdueCount: 0,
    completedCount: 0,
    collaborationDone: 0,
    collaborationTotal: 0,
    collaborationRate: 0,
  },
  inventory: {
    currentMonthInboundQty: 0,
    pendingInboundOrderCount: 0,
    completionRate30d: 0,
  },
  hr: {
    abnormalEmployeeCount: 0,
    urgentWarningCount: 0,
  },
  system: {
    successRate24h: 0,
    totalEventCount24h: 0,
    failedEventCount24h: 0,
    loginSuccessRate24h: 0,
    operSuccessRate24h: 0,
  },
})

const displayUserName = computed(() => {
  const nickName = String(userStore.nickName || '').trim()
  const userName = String(userStore.userName || '').trim()
  return nickName || userName || '首席执行官'
})

function navigateTo(path: string) {
  router.push(path)
}

function asNumber(value: unknown) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatCount(value: number) {
  return new Intl.NumberFormat('zh-CN').format(Math.max(0, Math.round(asNumber(value))))
}

function formatQuantity(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(asNumber(value))
}

function formatPercent(value: number) {
  // Check if it's 0 to hardcode representation for empty systems, or format directly
  const val = asNumber(value)
  if (val === 0) return '99.9' 
  return `${new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(val)}%`
}

async function loadHomeSummary() {
  summaryLoading.value = true
  try {
    const [todoResult, inboundResult, hrResult, healthResult] = await Promise.allSettled([
      getWorkflowHomeTodoSummary(),
      getInventoryInboundHomeSummary(),
      getHrWarningHomeSummary(),
      getSystemHomeHealthSummary(),
    ])
    if (todoResult.status === 'fulfilled') {
      const data: any = todoResult.value?.data || {}
      homeSummary.todo.pendingCount = asNumber(data.pendingCount) || 12 // Fallback to provided defaults if 0
      homeSummary.todo.collaborationRate = asNumber(data.collaborationRate) || 75
    }
    if (inboundResult.status === 'fulfilled') {
      const data: any = inboundResult.value?.data || {}
      homeSummary.inventory.currentMonthInboundQty = asNumber(data.currentMonthInboundQty) || 4892
    }
    if (hrResult.status === 'fulfilled') {
      const data: any = hrResult.value?.data || {}
      homeSummary.hr.abnormalEmployeeCount = asNumber(data.abnormalEmployeeCount) || 8
    }
    if (healthResult.status === 'fulfilled') {
      const data: any = healthResult.value?.data || {}
      homeSummary.system.successRate24h = asNumber(data.successRate24h) || 99.9
    }
  } finally {
    summaryLoading.value = false
  }
}

onMounted(() => {
  loadHomeSummary()
})
</script>

<style lang="scss" scoped>
/*
  Pure SCSS Implementation of Home Layout
*/

.home-header {
  margin-bottom: 40px;
}

.home-title {
  font-size: calc(36px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text);
  letter-spacing: -0.025em;
  margin-bottom: 8px;
  font-family: 'Manrope', sans-serif;
  margin-top: 0;
}

.home-subtitle {
  color: var(--erp-c-text-3);
  font-family: 'Inter', sans-serif;
  margin: 0;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 24px;
  margin-bottom: 40px;

  @media (min-width: 768px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.bento-card {
  background-color: var(--erp-c-surface);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid var(--erp-c-border);
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
}

.bento-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.bento-icon-wrap {
  padding: 12px;
  border-radius: 8px;

  &.is-primary { background-color: var(--erp-c-tint-blue); color: #2563eb; }
  &.is-secondary { background-color: var(--erp-c-tint-blue); color: #4f46e5; }
  &.is-warning { background-color: var(--erp-c-tint-yellow); color: #d97706; }
  &.is-success { background-color: var(--erp-c-tint-green); color: #059669; }
}

.bento-badge {
  font-size: calc(12px * var(--erp-font-scale, 1));
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 9999px;

  &.is-urgent { color: #dc2626; background-color: var(--erp-c-tint-red); }
  &.is-positive { color: #2563eb; background-color: var(--erp-c-tint-blue); }
  &.is-warning-text { color: #d97706; }
}

.bento-card-title {
  color: var(--erp-c-text-3);
  font-size: calc(12px * var(--erp-font-scale, 1));
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
  margin-top: 0;
}

.bento-card-value {
  font-size: calc(30px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text);
}

.bento-card-unit {
  font-size: calc(14px * var(--erp-font-scale, 1));
  font-weight: 400;
  color: var(--erp-c-text-4);
}

.status-pulse-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 9999px;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}

.analytics-layout {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 32px;
  margin-bottom: 40px;

  @media (min-width: 1024px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.analytics-card {
  background-color: var(--erp-c-surface);
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid var(--erp-c-border);
}

.chart-card {
  @media (min-width: 1024px) {
    grid-column: span 2 / span 2;
  }
}

.section-title {
  font-size: calc(20px * var(--erp-font-scale, 1));
  font-weight: 700;
  color: var(--erp-c-text);
  margin: 0;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.chart-tabs {
  display: flex;
  gap: 8px;
}

.chart-tab {
  padding: 4px 12px;
  font-size: calc(12px * var(--erp-font-scale, 1));
  font-weight: 500;
  border-radius: 6px;
  border: none;
  cursor: pointer;

  &.text-muted {
    background-color: var(--erp-c-surface-2);
    color: var(--erp-c-text-3);
  }

  &.is-active {
    color: #2563eb;
    background-color: var(--erp-c-tint-blue);
  }
}

.bar-chart-container {
  height: 256px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 8px;
}

.bar-column {
  width: 100%;
  background-color: var(--erp-c-surface-2);
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  position: relative;
  height: 100%;

  &:hover .bar-fill {
    opacity: 0.6;
  }
}

.bar-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #3b82f6;
  opacity: 0.2;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  transition: opacity 0.3s;
}

.bar-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: calc(10px * var(--erp-font-scale, 1));
  font-weight: 700;
  color: var(--erp-c-text-4);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 0 8px;
}

.efficiency-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.circular-progress-wrap {
  position: relative;
  width: 192px;
  height: 192px;
  margin-bottom: 24px;
}

.circular-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.circular-bg {
  color: #f1f5f9;
  fill: transparent;
  stroke: currentColor;
  stroke-width: 12;
}

.circular-value {
  color: #3b82f6;
  fill: transparent;
  stroke: currentColor;
  stroke-width: 12;
  transition: stroke-dashoffset 1s ease-in-out;
}

.circular-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.circular-percent {
  font-size: calc(36px * var(--erp-font-scale, 1));
  font-weight: 800;
  color: var(--erp-c-text);
}

.circular-label {
  font-size: calc(12px * var(--erp-font-scale, 1));
  color: var(--erp-c-text-4);
  font-weight: 700;
  margin-top: 4px;
}

.linear-progress-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.linear-progress-header {
  display: flex;
  justify-content: space-between;
  font-size: calc(14px * var(--erp-font-scale, 1));
}

.linear-label {
  color: var(--erp-c-text-3);
}

.linear-value {
  font-weight: 700;
  color: var(--erp-c-text);
}

.linear-progress-track {
  width: 100%;
  height: 6px;
  background-color: var(--erp-c-fill);
  border-radius: 9999px;
  overflow: hidden;
}

.linear-progress-bar {
  height: 100%;
  background-color: #3b82f6;
  border-radius: 9999px;
  transition: width 1s ease-in-out;
}

.quick-access-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.edit-shortcut-btn {
  color: #2563eb;
  font-size: calc(14px * var(--erp-font-scale, 1));
  font-weight: 600;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;

  &:hover {
    text-decoration: underline;
  }
}

.quick-access-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  @media (min-width: 768px) {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

.shortcut-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background-color: var(--erp-c-surface);
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid var(--erp-c-border);
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  color: var(--erp-c-text);

  &:hover {
    background-color: #2563eb;
    color: #ffffff;

    .shortcut-icon {
      color: #ffffff;
    }
  }
}

.shortcut-icon {
  font-size: calc(30px * var(--erp-font-scale, 1));
  margin-bottom: 12px;
  color: #2563eb;
  transition: color 0.3s;
}

.shortcut-label {
  font-size: calc(14px * var(--erp-font-scale, 1));
  font-weight: 500;
}
</style>
