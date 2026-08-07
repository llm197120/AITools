<route lang="json5">
{ style: { navigationBarTitleText: '日常计划' } }
</route>

<template>
  <view class="page">
    <view class="month-bar">
      <text class="nav-btn" @click="prevMonth">‹</text>
      <text class="month-label">{{ yearMonthLabel }}</text>
      <text class="nav-btn" @click="nextMonth">›</text>
    </view>

    <view class="week-head">
      <text v-for="w in weekLabels" :key="w" class="week-cell">{{ w }}</text>
    </view>
    <view class="calendar-grid">
      <view
        v-for="(cell, idx) in calendarCells"
        :key="idx"
        class="day-cell"
        :class="{ empty: !cell.date, today: cell.isToday, selected: cell.date === selectedDate, hasPlan: cell.hasPlan, hasExpired: cell.hasExpired, expiredOnly: cell.expiredOnly }"
        @click="cell.date && selectDate(cell.date)"
      >
        <text v-if="cell.date" class="day-num">{{ cell.day }}</text>
        <view v-if="cell.hasPlan" class="dot" :class="{ expired: cell.expiredOnly }"></view>
      </view>
    </view>

    <view class="list-header">
      <text>{{ selectedDate }} · {{ plans.length }} 项计划</text>
      <text class="add-link" @click="goAdd">+ 新增</text>
    </view>

    <view
      class="plan-item"
      v-for="p in plans"
      :key="p.id"
      :class="{ done: p.status === 'completed', expired: p.status === 'expired' }"
      @click="goDetail(p)"
    >
      <view class="plan-left">
        <view class="check" :class="{ checked: p.status === 'completed' }" @click.stop="togglePlan(p)"></view>
        <view>
          <text class="plan-title">{{ p.title }}</text>
          <text class="plan-cat">{{ p.category || '-' }} · {{ statusLabel(p.status) }}</text>
        </view>
      </view>
      <view class="priority-dot" :class="p.priority || 'normal'"></view>
    </view>

    <view v-if="plans.length === 0" class="empty">当日暂无计划</view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { planApi } from '../../pages-homeai/api/index'

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const selectedDate = ref(new Date().toISOString().substring(0, 10))
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const planDates = ref<string[]>([])
const expiredDates = ref<string[]>([])
const pendingDates = ref<string[]>([])
const plans = ref<any[]>([])

const yearMonthLabel = computed(() => `${viewYear.value}年${viewMonth.value}月`)
const yearMonthParam = computed(() => `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}`)

const calendarCells = computed(() => {
  const first = new Date(viewYear.value, viewMonth.value - 1, 1)
  const lastDay = new Date(viewYear.value, viewMonth.value, 0).getDate()
  const startWeek = first.getDay()
  const today = new Date().toISOString().substring(0, 10)
  const cells: any[] = []
  for (let i = 0; i < startWeek; i++) cells.push({})
  for (let d = 1; d <= lastDay; d++) {
    const date = `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({
      date,
      day: d,
      isToday: date === today,
      hasPlan: planDates.value.includes(date),
      hasExpired: expiredDates.value.includes(date),
      expiredOnly: expiredDates.value.includes(date) && !pendingDates.value.includes(date),
    })
  }
  return cells
})

function statusLabel(s: string) {
  return { pending: '待完成', completed: '已完成', expired: '已过期', cancelled: '已取消' }[s] || s
}

async function loadCalendar() {
  const res: any = await planApi.calendar(yearMonthParam.value)
  if (Array.isArray(res)) {
    planDates.value = res.map((d: any) => (typeof d === 'string' ? d : String(d).substring(0, 10)))
    expiredDates.value = []
    pendingDates.value = []
    return
  }
  planDates.value = (res?.dates || []).map((d: any) => String(d).substring(0, 10))
  expiredDates.value = (res?.expiredDates || []).map((d: any) => String(d).substring(0, 10))
  pendingDates.value = (res?.pendingDates || []).map((d: any) => String(d).substring(0, 10))
}

async function loadPlans() {
  plans.value = (await planApi.byDate(selectedDate.value)) || []
}

function selectDate(date: string) {
  selectedDate.value = date
  loadPlans()
}

function prevMonth() {
  if (viewMonth.value === 1) {
    viewMonth.value = 12
    viewYear.value--
  } else viewMonth.value--
  loadCalendar()
}

function nextMonth() {
  if (viewMonth.value === 12) {
    viewMonth.value = 1
    viewYear.value++
  } else viewMonth.value++
  loadCalendar()
}

async function togglePlan(p: any) {
  if (p.status === 'expired') {
    uni.showToast({ title: '已过期计划不可切换', icon: 'none' })
    return
  }
  await planApi.toggle(p.id)
  await loadPlans()
  await loadCalendar()
}

function goAdd() {
  uni.navigateTo({ url: `/pages-homeai-more/plan/add?planDate=${selectedDate.value}` })
}

function goDetail(p: any) {
  uni.navigateTo({
    url: `/pages-homeai-more/plan/detail?id=${p.id}&planDate=${selectedDate.value}`,
  })
}

onShow(async () => {
  await loadCalendar()
  await loadPlans()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; padding-bottom: 40rpx; }
.month-bar { display: flex; align-items: center; justify-content: center; gap: 40rpx; padding: 16rpx 0 24rpx; }
.nav-btn { font-size: 40rpx; color: #667eea; padding: 0 20rpx; }
.month-label { font-size: 32rpx; font-weight: 600; color: #333; }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.week-cell { font-size: 22rpx; color: #999; }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: #fff; border-radius: 16rpx; padding: 16rpx; margin-bottom: 20rpx; }
.day-cell { aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 12rpx; position: relative; }
.day-cell.empty { visibility: hidden; }
.day-cell.selected { background: #667eea; }
.day-cell.selected .day-num { color: #fff; }
.day-cell.today:not(.selected) { border: 2rpx solid #667eea; }
.day-num { font-size: 26rpx; color: #333; }
.dot { width: 8rpx; height: 8rpx; border-radius: 50%; background: #667eea; margin-top: 4rpx; }
.dot.expired { background: #bbb; }
.day-cell.selected .dot { background: #fff; }
.day-cell.hasExpired:not(.selected) .day-num { color: #aaa; }
.day-cell.expiredOnly:not(.selected) { background: #f0f0f0; }
.list-header { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 8rpx 16rpx; font-size: 26rpx; color: #666; }
.add-link { color: #667eea; }
.plan-item { display: flex; align-items: center; justify-content: space-between; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; }
.plan-item.done .plan-title { text-decoration: line-through; color: #999; }
.plan-item.expired { opacity: 0.6; }
.plan-left { display: flex; align-items: center; gap: 16rpx; flex: 1; }
.check { width: 32rpx; height: 32rpx; border: 2rpx solid #ccc; border-radius: 50%; }
.check.checked { background: #52c41a; border-color: #52c41a; }
.plan-title { font-size: 28rpx; color: #333; display: block; }
.plan-cat { font-size: 22rpx; color: #999; display: block; }
.priority-dot { width: 16rpx; height: 16rpx; border-radius: 50%; flex-shrink: 0; }
.priority-dot.normal { background: #999; }
.priority-dot.important { background: #f39c12; }
.priority-dot.urgent { background: #e74c3c; }
.empty { text-align: center; color: #999; padding: 40rpx; font-size: 26rpx; }
</style>
