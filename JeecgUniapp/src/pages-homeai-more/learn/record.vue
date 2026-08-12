<route lang="json5">{ style: { navigationBarTitleText: '学习记录', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page">
    <view class="month-bar">
      <text class="nav" @click="prevMonth">‹</text>
      <text class="month">{{ viewYear }}年{{ viewMonth }}月</text>
      <text class="nav" @click="nextMonth">›</text>
    </view>
    <view class="week-head">
      <text v-for="w in weekLabels" :key="w" class="w">{{ w }}</text>
    </view>
    <view class="grid">
      <view v-for="(cell, i) in calendarCells" :key="i" class="cell" :class="{ empty: !cell.date, studied: cell.studied }">
        <text v-if="cell.date">{{ cell.day }}</text>
      </view>
    </view>
    <view class="records">
      <view class="record" v-for="r in records" :key="r.id">
        <text class="title">{{ r.materialTitle || r.title || '学习记录' }}</text>
        <text class="meta">{{ formatDuration(r.duration) }} · {{ formatDate(r.startTime || r.createTime) }}</text>
        <text v-if="r.notes" class="notes">{{ r.notes }}</text>
      </view>
      <HomeEmpty v-if="records.length === 0" title="暂无学习记录" />
    </view>
  </view>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { learnApi } from '../../pages-homeai/api/learn'
import HomeEmpty from '../../components/HomeEmpty.vue'

const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const studyDates = ref<string[]>([])
const records = ref<any[]>([])
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']

const yearMonthParam = computed(() => `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}`)

const calendarCells = computed(() => {
  const first = new Date(viewYear.value, viewMonth.value - 1, 1)
  const lastDay = new Date(viewYear.value, viewMonth.value, 0).getDate()
  const startWeek = first.getDay()
  const cells: any[] = []
  for (let i = 0; i < startWeek; i++) cells.push({})
  for (let d = 1; d <= lastDay; d++) {
    const date = `${yearMonthParam.value}-${String(d).padStart(2, '0')}`
    cells.push({ date, day: d, studied: studyDates.value.includes(date) })
  }
  return cells
})

function formatDuration(sec: number) {
  const m = Math.floor((sec || 0) / 60)
  return m > 0 ? `${m} 分钟` : `${sec || 0} 秒`
}

function formatDate(t: string) {
  return t ? String(t).substring(0, 16).replace('T', ' ') : ''
}

async function loadData() {
  studyDates.value = (await learnApi.calendar(yearMonthParam.value)) || []
  const all = (await learnApi.records()) || []
  records.value = all.filter((r: any) => {
    const d = String(r.startTime || r.createTime || '').substring(0, 7)
    return d === yearMonthParam.value
  })
}

function prevMonth() {
  if (viewMonth.value === 1) { viewMonth.value = 12; viewYear.value-- } else viewMonth.value--
  loadData()
}

function nextMonth() {
  if (viewMonth.value === 12) { viewMonth.value = 1; viewYear.value++ } else viewMonth.value++
  loadData()
}

onShow(loadData)
</script>
<style scoped>
/* page shell: .hai-page */
.month-bar { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 12rpx 0 20rpx; }
.nav { font-size: 36rpx; color: var(--hai-primary); }
.month { font-size: 30rpx; font-weight: 600; color: var(--hai-text); }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.w { font-size: 22rpx; color: var(--hai-text-muted); }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: var(--hai-card); border-radius: 28rpx; padding: 16rpx; margin-bottom: 20rpx; box-shadow: var(--hai-shadow); }
.cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 8rpx; font-size: 24rpx; color: var(--hai-text); }
.cell.empty { visibility: hidden; }
.cell.studied { background: var(--hai-success-soft); color: var(--hai-success); font-weight: 600; }
.records { background: var(--hai-card); border-radius: 28rpx; padding: 16rpx; box-shadow: var(--hai-shadow); }
.record { padding: 20rpx 8rpx; border-bottom: 1rpx solid var(--hai-border); }
.title { font-size: 28rpx; color: var(--hai-text); display: block; }
.meta { font-size: 22rpx; color: var(--hai-text-muted); display: block; margin-top: 6rpx; }
.notes { font-size: 24rpx; color: var(--hai-text-secondary); display: block; margin-top: 8rpx; }
</style>
