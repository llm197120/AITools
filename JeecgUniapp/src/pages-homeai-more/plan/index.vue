<route lang="json5">
{ style: { navigationBarTitleText: '日常计划', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true } }
</route>

<template>
  <view class="hai-page">
    <OfflineBanner />
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

    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索当日计划标题" confirm-type="search" />
      <text v-if="keyword" class="search-clear" @click="keyword = ''">清除</text>
    </view>

    <view class="list-header">
      <text>{{ selectedDate }} · {{ displayedPlans.length }} 项计划</text>
      <text class="add-link" @click="goAdd">+ 新增</text>
    </view>

    <view
      class="plan-item"
      v-for="p in displayedPlans"
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

    <view v-if="loading && plans.length === 0"><HomeSkeleton variant="list" :rows="3" /></view>
    <HomeEmpty
      v-else-if="loadFailed"
      title="计划加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      @action="reloadDay"
    />
    <HomeEmpty
      v-else-if="!loading && displayedPlans.length === 0"
      :title="keyword.trim() ? '未找到相关计划' : '当日暂无计划'"
      :hint="keyword.trim() ? '换个词试试，或清空搜索' : '安排一条计划，开始今天'"
      :action-text="keyword.trim() ? '清空搜索' : '+ 新增'"
      @action="keyword.trim() ? (keyword = '') : goAdd()"
    />
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { planApi } from '../../pages-homeai/api/index'
import { localDateStr, toDateStr } from '../../pages-homeai/utils/date'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { readList, mutate } from '../../pages-homeai/offline/dataAccess'
import OfflineBanner from '../../pages-homeai/offline/OfflineBanner.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  await loadCalendar()
  await loadPlans()
})

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const selectedDate = ref(localDateStr())
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const planDates = ref<string[]>([])
const expiredDates = ref<string[]>([])
const pendingDates = ref<string[]>([])
const plans = ref<any[]>([])
const keyword = ref('')
const loadFailed = ref(false)
const loading = ref(true)
const displayedPlans = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return plans.value
  return plans.value.filter((p) => String(p.title || '').includes(kw))
})

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
  try {
    const res: any = await readList(
      'plan',
      'calendar:' + yearMonthParam.value,
      () => planApi.calendar(yearMonthParam.value),
    ).then((r) => r.data)
    if (Array.isArray(res)) {
      planDates.value = res.map((d: any) => toDateStr(d)).filter(Boolean)
      expiredDates.value = []
      pendingDates.value = []
      return
    }
    planDates.value = (res?.dates || []).map((d: any) => toDateStr(d)).filter(Boolean)
    expiredDates.value = (res?.expiredDates || []).map((d: any) => toDateStr(d)).filter(Boolean)
    pendingDates.value = (res?.pendingDates || []).map((d: any) => toDateStr(d)).filter(Boolean)
  } catch {
    uni.showToast({ title: '日历加载失败', icon: 'none' })
  }
}

async function loadPlans() {
  const silent = plans.value.length > 0
  if (!silent) loading.value = true
  loadFailed.value = false
  try {
    const res = await readList<any[]>(
      'plan',
      'byDate:' + selectedDate.value,
      () => planApi.byDate(selectedDate.value),
    )
    plans.value = res.data || []
    if (res.offline) {
      uni.showToast({ title: '离线模式，展示本地数据', icon: 'none' })
    }
  } catch {
    if (!silent) plans.value = []
    loadFailed.value = plans.value.length === 0
    if (silent && plans.value.length > 0) {
      uni.showToast({ title: '刷新失败', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

async function reloadDay() {
  await loadCalendar()
  await loadPlans()
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

const togglingId = ref('')

async function togglePlan(p: any) {
  if (p.status === 'expired') {
    uni.showToast({ title: '已过期计划不可切换', icon: 'none' })
    return
  }
  if (togglingId.value) return
  togglingId.value = p.id
  try {
    const nextStatus = p.status === 'completed' ? 'pending' : 'completed'
    const res = await mutate(
      'plan',
      'toggle',
      { instanceId: p.id },
      (payload) => planApi.toggle(payload.instanceId),
    )
    if (res.queued) {
      // 离线/网络失败：乐观更新本地
      p.status = nextStatus
      uni.showToast({ title: '已离线保存，联网后同步', icon: 'none' })
      await loadCalendar()
    } else {
      await loadPlans()
      await loadCalendar()
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  } finally {
    togglingId.value = ''
  }
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
.search-bar{position:relative;padding:8rpx 0 16rpx}
.search-input{height:72rpx;padding:0 88rpx 0 28rpx;background:var(--hai-card);border-radius:999rpx;font-size:28rpx;box-shadow:var(--hai-shadow)}
.search-clear{position:absolute;right:28rpx;top:50%;transform:translateY(-50%);font-size:24rpx;color:var(--hai-primary)}
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
