<route lang="json5">
{ style: { navigationBarTitleText: '日常计划', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="hai-page">
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
        <view class="check-hit" @click.stop="togglePlan(p)">
          <view class="check" :class="{ checked: p.status === 'completed' }"></view>
        </view>
        <view>
          <text class="plan-title">{{ p.title }}</text>
          <text class="plan-cat">{{ p.category || '-' }} · {{ statusLabel(p.status) }}</text>
        </view>
      </view>
      <view class="priority-dot" :class="p.priority || 'normal'"></view>
    </view>

    <HomeEmpty
      v-if="plans.length === 0"
      title="当日暂无计划"
      hint="安排一条计划，开始今天"
      action-text="+ 新增"
      @action="goAdd"
    />
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { planApi } from '../../pages-homeai/api/index'
import { localDateStr } from '../../pages-homeai/utils/date'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const selectedDate = ref(localDateStr())
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
  const today = localDateStr()
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
/* page shell: .hai-page */
.month-bar { display: flex; align-items: center; justify-content: center; gap: 40rpx; padding: 8rpx 0 24rpx; }
.nav-btn { font-size: 40rpx; color: var(--hai-primary); padding: 0 20rpx; }
.month-label { font-family: var(--hai-serif); font-size: 32rpx; font-weight: 700; color: var(--hai-text); }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.week-cell { font-size: 22rpx; color: var(--hai-text-muted); }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: var(--hai-card); border-radius: var(--hai-radius); padding: 20rpx; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.day-cell { aspect-ratio: 1; min-height: 72rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 16rpx; position: relative; }
.day-cell.empty { visibility: hidden; }
.day-cell.selected { background: var(--hai-primary); }
.day-cell.selected .day-num { color: var(--hai-on-primary); }
.day-cell.today:not(.selected) { border: 2rpx solid var(--hai-primary); background: var(--hai-primary-soft); }
.day-num { font-size: 26rpx; color: var(--hai-text); }
.dot { width: 8rpx; height: 8rpx; border-radius: 50%; background: var(--hai-primary); margin-top: 4rpx; }
.dot.expired { background: var(--hai-text-tertiary); }
.day-cell.selected .dot { background: var(--hai-card); }
.day-cell.hasExpired:not(.selected) .day-num { color: var(--hai-text-muted); }
.day-cell.expiredOnly:not(.selected) { background: var(--hai-border); }
.list-header { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 8rpx 16rpx; font-size: 26rpx; color: var(--hai-text-secondary); }
.add-link { color: var(--hai-primary); }
.plan-item { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 28rpx; background: var(--hai-card); border-radius: var(--hai-radius-md); margin-bottom: 16rpx; box-shadow: var(--hai-shadow); }
.plan-item.done .plan-title { text-decoration: line-through; color: var(--hai-text-muted); }
.plan-item.expired { opacity: 0.6; }
.plan-left { display: flex; align-items: center; gap: 8rpx; flex: 1; min-width: 0; }
.check-hit {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin: -12rpx 0 -12rpx -16rpx;
}
.check { width: 40rpx; height: 40rpx; border: 2rpx solid var(--hai-text-tertiary); border-radius: 50%; }
.check.checked { background: var(--hai-success); border-color: var(--hai-success); }
.plan-title { font-size: 28rpx; color: var(--hai-text); display: block; }
.plan-cat { font-size: 22rpx; color: var(--hai-text-muted); display: block; }
.priority-dot { width: 16rpx; height: 16rpx; border-radius: 50%; flex-shrink: 0; }
.priority-dot.normal { background: var(--hai-text-muted); }
.priority-dot.important { background: var(--hai-warning); }
.priority-dot.urgent { background: var(--hai-danger); }
</style>
