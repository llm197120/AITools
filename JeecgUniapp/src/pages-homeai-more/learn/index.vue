<route lang="json5">
{ style: { navigationBarTitleText: '学习模块' } }
</route>

<template>
  <view class="page">
    <view class="stats-card">
      <text class="stats-num">{{ stats.totalRecords || 0 }}</text>
      <text class="stats-label">次学习记录</text>
      <text class="stats-num">{{ stats.totalDuration || 0 }}</text>
      <text class="stats-label">总时长(分钟)</text>
    </view>

    <view v-if="activeSession.materialId" class="learn-bar">
      <view class="learn-bar-info">
        <text class="learn-bar-title">正在学习：{{ activeSession.materialTitle }}</text>
        <text class="learn-bar-time">{{ formatElapsed(displayElapsed) }}</text>
      </view>
      <view class="learn-bar-stop" @click="stopLearn">结束</view>
    </view>

    <view class="tab-bar">
      <text :class="['tab', activeTab === 'list' ? 'active' : '']" @click="activeTab = 'list'">资料列表</text>
      <text :class="['tab', activeTab === 'calendar' ? 'active' : '']" @click="switchCalendar">学习日历</text>
      <text class="tab link" @click="goRecord">记录 ›</text>
    </view>

    <view v-if="activeTab === 'calendar'" class="calendar-wrap">
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
      <text class="cal-tip">绿色日期表示当天有学习记录</text>
    </view>

    <template v-else>
      <view class="material-item" v-for="m in materials" :key="m.id" @click="startLearn(m)">
        <view class="mat-icon">{{ getTypeIcon(m.type) }}</view>
        <view class="mat-info">
          <text class="mat-title">{{ m.title }}</text>
          <text class="mat-cat">{{ m.category }} · {{ m.type }}</text>
        </view>
        <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
      </view>
      <view v-if="materials.length === 0" class="empty">暂无学习资料，点击右下角添加</view>
    </template>

    <view class="fab" @click="goAdd">+</view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { learnApi } from '../../pages-homeai/api/index'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'

const materials = ref<any[]>([])
const stats = ref<any>({})
const activeTab = ref('list')
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const studyDates = ref<string[]>([])
const activeSession = ref<any>({})
const displayElapsed = ref(0)
let timerHandle: ReturnType<typeof setInterval> | null = null
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']

const yearMonthParam = computed(() =>
  `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}`,
)

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

function getTypeIcon(t: string) {
  const m: any = { pdf: '📄', video: '🎬', link: '🔗', note: '📝', image: '🖼️', doc: '📝' }
  return m[t] || '📎'
}

function formatElapsed(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function clearTimer() {
  if (timerHandle) {
    clearInterval(timerHandle)
    timerHandle = null
  }
}

function startTimer(baseSeconds = 0) {
  clearTimer()
  displayElapsed.value = baseSeconds
  timerHandle = setInterval(() => {
    displayElapsed.value += 1
  }, 1000)
}

async function loadActiveSession() {
  const session: any = (await learnApi.activeSession()) || {}
  if (!session.materialId) {
    activeSession.value = {}
    clearTimer()
    return
  }
  activeSession.value = session
  startTimer(session.elapsedSeconds || 0)
}

async function loadData() {
  const page: any = await learnApi.materials(1, 100)
  materials.value = page?.records || page || []
  stats.value = (await learnApi.statistics()) || {}
  await loadActiveSession()
}

async function loadCalendar() {
  studyDates.value = (await learnApi.calendar(yearMonthParam.value)) || []
}

function switchCalendar() {
  activeTab.value = 'calendar'
  loadCalendar()
}

function prevMonth() {
  if (viewMonth.value === 1) { viewMonth.value = 12; viewYear.value-- } else viewMonth.value--
  loadCalendar()
}

function nextMonth() {
  if (viewMonth.value === 12) { viewMonth.value = 1; viewYear.value++ } else viewMonth.value++
  loadCalendar()
}

function startLearn(m: any) {
  uni.navigateTo({ url: `/pages-homeai-more/learn/detail?id=${m.id}` })
}

async function stopLearn() {
  if (!activeSession.value.materialId) return
  await learnApi.stop(activeSession.value.materialId)
  uni.showToast({ title: '已记录学习时长', icon: 'success' })
  activeSession.value = {}
  clearTimer()
  await loadData()
}

function goAdd() {
  uni.navigateTo({ url: '/pages-homeai-more/learn/add' })
}

function goRecord() {
  uni.navigateTo({ url: '/pages-homeai-more/learn/record' })
}

onShow(() => {
  preloadWhitelist()
  loadData()
})

onUnmounted(() => clearTimer())
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; padding-bottom: 120rpx; }
.learn-bar { display: flex; align-items: center; justify-content: space-between; background: #27ae60; color: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; margin-bottom: 16rpx; }
.learn-bar-info { flex: 1; }
.learn-bar-title { display: block; font-size: 26rpx; margin-bottom: 6rpx; }
.learn-bar-time { font-size: 36rpx; font-weight: 700; font-variant-numeric: tabular-nums; }
.learn-bar-stop { background: rgba(255,255,255,0.2); padding: 12rpx 24rpx; border-radius: 8rpx; font-size: 26rpx; }
.stats-card { display: flex; flex-wrap: wrap; background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.stats-num { width: 50%; font-size: 36rpx; font-weight: 700; text-align: center; }
.stats-label { width: 50%; font-size: 22rpx; color: #999; text-align: center; margin-bottom: 12rpx; }
.tab-bar { display: flex; background: #fff; border-radius: 12rpx; margin-bottom: 16rpx; overflow: hidden; }
.tab { flex: 1; text-align: center; padding: 20rpx; font-size: 28rpx; color: #666; }
.tab.active { color: #27ae60; font-weight: 600; border-bottom: 4rpx solid #27ae60; }
.tab.link { flex: 0.7; color: #667eea; font-size: 26rpx; border-bottom: none; }
.calendar-wrap { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.month-bar { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 12rpx 0 20rpx; }
.nav { font-size: 36rpx; color: #27ae60; }
.month { font-size: 30rpx; font-weight: 600; }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.w { font-size: 22rpx; color: #999; }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; }
.cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 8rpx; font-size: 24rpx; }
.cell.empty { visibility: hidden; }
.cell.studied { background: #d4edda; color: #27ae60; font-weight: 600; }
.cal-tip { display: block; text-align: center; font-size: 22rpx; color: #999; margin-top: 16rpx; }
.material-item { display: flex; align-items: center; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; gap: 16rpx; }
.mat-icon { font-size: 32rpx; }
.mat-info { flex: 1; }
.mat-title { font-size: 28rpx; display: block; }
.mat-cat { font-size: 22rpx; color: #999; display: block; }
.fab { position: fixed; right: 40rpx; bottom: 100rpx; width: 100rpx; height: 100rpx; background: #27ae60; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48rpx; color: #fff; }
.empty { text-align: center; color: #999; padding: 60rpx 0; font-size: 26rpx; }
</style>
