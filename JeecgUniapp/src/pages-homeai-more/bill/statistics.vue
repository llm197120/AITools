<route lang="json5">
{ style: { navigationBarTitleText: '统计分析', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="hai-page">
    <view class="month-select">
      <text @click="changeMonth(-1)">◀</text>
      <text class="month">{{ currentMonth }}</text>
      <text @click="changeMonth(1)">▶</text>
    </view>
    <view class="stats">
      <view class="stat-item"><text class="s-label">总支出</text><text class="s-value red">¥{{ summary.expense }}</text></view>
      <view class="stat-item"><text class="s-label">总收入</text><text class="s-value green">¥{{ summary.income }}</text></view>
      <view class="stat-item"><text class="s-label">结余</text><text class="s-value">¥{{ summary.balance }}</text></view>
    </view>
    <view class="section-title">支出分类占比</view>
    <view v-if="loading"><HomeSkeleton variant="list" :rows="3" /></view>
    <view v-else-if="categoryStats.length === 0"><HomeEmpty title="暂无统计数据" /></view>
    <view v-else class="cat-list">
      <view class="cat-row" v-for="c in categoryStats" :key="c.categoryId">
        <text class="cat-icon">{{ c.icon || '💰' }}</text>
        <view class="cat-bar-wrap">
          <text class="cat-name">{{ c.name || '其他' }}</text>
          <view class="cat-bar-bg"><view class="cat-bar" :style="{ width: c.percent + '%' }"></view></view>
        </view>
        <text class="cat-amt">¥{{ c.amount }}</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { localMonthStr } from '../../pages-homeai/utils/date'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'

const currentMonth = ref(localMonthStr())
const summary = ref({ expense: '0', income: '0', balance: '0' })
const categoryStats = ref<any[]>([])
const loading = ref(true)

async function loadStats() {
  loading.value = true
  try {
    const sum: any = await billApi.summary(currentMonth.value)
    summary.value = {
      expense: sum.totalExpense ?? '0',
      income: sum.totalIncome ?? '0',
      balance: sum.balance ?? '0',
    }
    const stats: any[] = (await billApi.categoryStats(currentMonth.value)) || []
    const total = stats.reduce((s, x) => s + Number(x.amount || 0), 0) || 1
    categoryStats.value = stats.map((x) => ({
      ...x,
      percent: Math.round((Number(x.amount || 0) / total) * 100),
    }))
  } finally {
    loading.value = false
  }
}

function changeMonth(d: number) {
  const [y, m] = currentMonth.value.split('-').map(Number)
  const dt = new Date(y, m - 1 + d)
  currentMonth.value = localMonthStr(dt)
  loadStats()
}

onShow(loadStats)
</script>

<style scoped>
/* page shell: .hai-page */
.month-select { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 30rpx; font-size: 32rpx; font-weight: 600; color: var(--hai-text); }
.stats { display: flex; gap: 16rpx; margin-bottom: 30rpx; }
.stat-item { flex: 1; background: var(--hai-card); border-radius: 24rpx; padding: 30rpx; text-align: center; box-shadow: var(--hai-shadow); }
.s-label { font-size: 22rpx; color: var(--hai-text-muted); }
.s-value { font-size: 36rpx; font-weight: 700; display: block; margin-top: 8rpx; color: var(--hai-text); }
.red { color: var(--hai-danger); }
.green { color: var(--hai-success); }
.section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; padding: 0 8rpx; color: var(--hai-text); }
.cat-list { background: var(--hai-card); border-radius: 28rpx; padding: 16rpx 24rpx; box-shadow: var(--hai-shadow); }
.cat-row { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 0; border-bottom: 1rpx solid var(--hai-border); }
.cat-row:last-child { border-bottom: none; }
.cat-icon { font-size: 32rpx; width: 40rpx; }
.cat-bar-wrap { flex: 1; }
.cat-name { font-size: 24rpx; color: var(--hai-text-secondary); display: block; margin-bottom: 8rpx; }
.cat-bar-bg { height: 12rpx; background: var(--hai-border); border-radius: 6rpx; overflow: hidden; }
.cat-bar { height: 100%; background: var(--hai-primary); border-radius: 6rpx; }
.cat-amt { font-size: 26rpx; font-weight: 600; color: var(--hai-text); min-width: 120rpx; text-align: right; }
</style>
