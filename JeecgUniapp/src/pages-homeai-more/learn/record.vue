<route lang="json5">{ style: { navigationBarTitleText: '学习记录' } }</route>
<template>
  <view class="page">
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
.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; }
.month-bar { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 12rpx 0 20rpx; }
.nav { font-size: 36rpx; color: #27ae60; }
.month { font-size: 30rpx; font-weight: 600; }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.w { font-size: 22rpx; color: #999; }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: #fff; border-radius: 12rpx; padding: 16rpx; margin-bottom: 20rpx; }
.cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 8rpx; font-size: 24rpx; }
.cell.empty { visibility: hidden; }
.cell.studied { background: #d4edda; color: #27ae60; font-weight: 600; }
.records { background: #fff; border-radius: 12rpx; padding: 16rpx; }
.record { padding: 20rpx 8rpx; border-bottom: 1rpx solid #f0f0f0; }
.title { font-size: 28rpx; color: #333; display: block; }
.meta { font-size: 22rpx; color: #999; display: block; margin-top: 6rpx; }
.notes { font-size: 24rpx; color: #666; display: block; margin-top: 8rpx; }
</style>
