<template>
  <div class="home-page erp-page">
    <section class="hero erp-panel">
      <div class="hero-content">
        <h2>数智协同运营驾驶舱</h2>
        <p>围绕合规、协同、创新、绿色发展构建统一管理视图，提升业务透明度与执行效率。</p>
        <div class="hero-tags">
          <span>合规治理</span>
          <span>数据透明</span>
          <span>协同运营</span>
          <span>绿色可持续</span>
        </div>
      </div>
      <div class="tenant-info">
        <div class="tenant-label">当前租户</div>
        <div class="tenant-value">{{ userStore.tenantId || 'DEFAULT' }}</div>
      </div>
    </section>

    <el-row :gutter="16">
      <el-col :xl="6" :lg="6" :md="12" :sm="12" :xs="24" v-for="item in statistics" :key="item.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-header">
            <el-icon :size="20" :color="item.color"><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </div>
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="overview-card" shadow="never">
      <template #header>
        <div class="overview-header">系统概览</div>
      </template>
      <p>系统已完成登录页与首页视觉统一，后续功能页将按同一设计语言持续迭代。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import { User, OfficeBuilding, Money, TrendCharts } from '@element-plus/icons-vue'

const userStore = useUserStore()

const statistics = [
  { title: '总用户数', value: '1,280', desc: '较昨日 +2.4%', icon: User, color: '#0d6fbe' },
  { title: '活跃租户', value: '45', desc: '本周新增 3 家', icon: OfficeBuilding, color: '#1cb9a0' },
  { title: '今日营收', value: '￥24,500', desc: '同比 +8.6%', icon: Money, color: '#ff8f4f' },
  { title: '月增长率', value: '12.5%', desc: '环比 +1.2%', icon: TrendCharts, color: '#2f66d0' }
]
</script>

<style scoped lang="scss">
.home-page {
  .hero {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 24px;
    background:
      linear-gradient(120deg, rgba(11, 46, 103, 0.9) 0%, rgba(16, 121, 167, 0.8) 100%),
      linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0));
    color: #f6fbff;

    .hero-content {
      h2 {
        margin: 0;
        font-size: clamp(24px, 2.8vw, 34px);
      }

      p {
        margin: 12px 0 0;
        max-width: 760px;
        line-height: 1.8;
        color: rgba(245, 251, 255, 0.9);
      }
    }

    .hero-tags {
      margin-top: 16px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      span {
        padding: 6px 12px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.18);
        font-size: 13px;
      }
    }

    .tenant-info {
      min-width: 180px;
      padding: 14px 16px;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.24);
      background: rgba(255, 255, 255, 0.12);
      align-self: flex-start;
      text-align: right;

      .tenant-label {
        font-size: 13px;
        color: rgba(248, 252, 255, 0.8);
      }

      .tenant-value {
        margin-top: 6px;
        font-size: 22px;
        font-weight: 700;
      }
    }
  }
}

.stat-card {
  min-height: 152px;
  margin-top: 4px;

  .stat-header {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #5d6f85;
    margin-bottom: 12px;
    font-weight: 600;
  }

  .stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #1f2f46;
    line-height: 1.2;
  }

  .stat-desc {
    margin-top: 10px;
    color: #6f8298;
    font-size: 13px;
  }
}

.overview-card {
  p {
    margin: 0;
    color: #40556f;
    line-height: 1.8;
  }
}

.overview-header {
  font-size: 16px;
  color: #1f2f46;
  font-weight: 700;
}

@media (max-width: 900px) {
  .home-page {
    .hero {
      flex-direction: column;
      padding: 18px;

      .tenant-info {
        width: fit-content;
        min-width: 140px;
        text-align: left;
      }
    }
  }
}
</style>
