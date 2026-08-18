<route lang="json5">
{
  style: {
    navigationBarTitleText: '学习模块',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
  },
}
</route>

<template>
  <view class="hai-page hai-page--fab">
    <view class="stats-card">
      <text class="stats-num">{{ stats.totalRecords || 0 }}</text>
      <text class="stats-label">次学习记录</text>
      <text class="stats-num">{{ stats.totalDuration || 0 }}</text>
      <text class="stats-label">总时长(分钟)</text>
    </view>

    <view class="goal-card" @click="editGoal">
      <view class="goal-head">
        <text class="goal-title">今日目标</text>
        <text class="goal-action">设置</text>
      </view>
      <view class="goal-progress-row">
        <text class="goal-num">{{ goal.todayMinutes || 0 }}</text>
        <text class="goal-sep">/</text>
        <text class="goal-num">{{ goal.goalMinutes || 30 }}</text>
        <text class="goal-unit">分钟</text>
      </view>
      <view class="goal-bar">
        <view class="goal-bar-inner" :style="{ width: Math.min(100, goal.progressPercent || 0) + '%' }"></view>
      </view>
      <text class="goal-tip">{{ goal.reached ? '今日目标已达成' : '每晚 20:00 未达标将提醒（需授权订阅）' }}</text>
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
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <HomeEmpty
        v-if="materials.length === 0 && !loadingMore"
        title="暂无学习资料"
        hint="添加资料后即可开始计时学习"
        action-text="添加资料"
        @action="goAdd"
      />
      <!-- 底部加载更多：配合 onReachBottom，部分端页面滚动不触发时可用按钮兜底 -->
      <view v-if="materials.length > 0" class="load-more-wrap">
        <view v-if="loadingMore" class="load-more-tip">加载中...</view>
        <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
        <view v-else class="load-more-tip">没有更多了</view>
      </view>
    </template>

    <view class="hai-fab" @click="goAdd">
      <wd-icon name="add" size="24px" color="#fff" />
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref, onUnmounted } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { learnApi, configApi } from '../../pages-homeai/api/index'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const PAGE_SIZE = 20
const materials = ref<any[]>([])
const stats = ref<any>({})
const goal = ref<any>({ goalMinutes: 30, todayMinutes: 0, progressPercent: 0, reached: false })
const activeTab = ref('list')
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const studyDates = ref<string[]>([])
const activeSession = ref<any>({})
const displayElapsed = ref(0)
const pageNo = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
let timerHandle: ReturnType<typeof setInterval> | null = null
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const GOAL_OPTIONS = [15, 30, 45, 60, 90]

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

/** 拉取一页资料；reset=true 时重置为第一页 */
async function fetchMaterials(reset = false) {
  if (loadingMore.value) return
  if (!reset && !hasMore.value) return

  loadingMore.value = true
  try {
    const nextPage = reset ? 1 : pageNo.value + 1
    const page: any = await learnApi.materials(nextPage, PAGE_SIZE)
    const records: any[] = page?.records || (Array.isArray(page) ? page : [])
    if (reset) {
      materials.value = records
    } else {
      materials.value = materials.value.concat(records)
    }
    pageNo.value = nextPage
    // 不足一页或明确无更多
    const total = page?.total
    if (typeof total === 'number') {
      hasMore.value = materials.value.length < total
    } else {
      hasMore.value = records.length >= PAGE_SIZE
    }
  } finally {
    loadingMore.value = false
  }
}

async function loadGoal() {
  try {
    goal.value = (await learnApi.goal()) || goal.value
  } catch {
    // 目标加载失败不阻断页面
  }
}

async function requestLearnSubscribe() {
  try {
    const cfg: any = await configApi.wechatPublic()
    const tmplId = cfg?.learnRemindTemplateId
    if (!tmplId) return
    // #ifdef MP-WEIXIN
    await new Promise<void>((resolve) => {
      uni.requestSubscribeMessage({
        tmplIds: [tmplId],
        complete: () => resolve(),
      })
    })
    // #endif
    // #ifdef APP-PLUS
    // TODO【Android迁移】本地通知/厂商推送后续迭代实现，当前 App 端暂无提醒推送
    // #endif
  } catch {
    // 订阅失败不阻断
  }
}

function editGoal() {
  uni.showActionSheet({
    itemList: GOAL_OPTIONS.map((m) => `每日 ${m} 分钟`),
    success: async (res) => {
      const minutes = GOAL_OPTIONS[res.tapIndex]
      if (!minutes) return
      await requestLearnSubscribe()
      goal.value = (await learnApi.setGoal(minutes)) || goal.value
      uni.showToast({ title: '目标已更新', icon: 'success' })
    },
  })
}

// 统计/目标短 TTL 缓存：避免频繁返回本页重复拉取（列表与进行中会话保持实时）
const LEARN_STATS_TTL = 30 * 1000
let lastStatsAt = 0

async function loadData() {
  pageNo.value = 1
  hasMore.value = true
  await fetchMaterials(true)
  if (Date.now() - lastStatsAt >= LEARN_STATS_TTL) {
    stats.value = (await learnApi.statistics()) || {}
    await loadGoal()
    lastStatsAt = Date.now()
  }
  await loadActiveSession()
}

async function loadMore() {
  if (activeTab.value !== 'list') return
  await fetchMaterials(false)
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
  uni.navigateTo({ url: `/pages-homeai-more/learn/detail?id=${m.id}&autoStart=1` })
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

onReachBottom(() => {
  loadMore()
})

onUnmounted(() => clearTimer())
</script>

<style scoped>
/* page shell: .hai-page */
.learn-bar { display: flex; align-items: center; justify-content: space-between; background: var(--hai-success); color: var(--hai-on-primary); border-radius: 28rpx; padding: 24rpx 28rpx; margin-bottom: 16rpx; box-shadow: var(--hai-shadow); }
.learn-bar-info { flex: 1; }
.learn-bar-title { display: block; font-size: 26rpx; margin-bottom: 6rpx; }
.learn-bar-time { font-size: 36rpx; font-weight: 700; font-variant-numeric: tabular-nums; }
.learn-bar-stop { background: rgba(255,255,255,0.2); padding: 12rpx 24rpx; border-radius: 999rpx; font-size: 26rpx; }
.stats-card { display: flex; flex-wrap: wrap; background: var(--hai-card); border-radius: 28rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--hai-shadow); }
.stats-num { width: 50%; font-family: 'Songti SC', 'STSong', serif; font-size: 36rpx; font-weight: 700; text-align: center; color: var(--hai-text); }
.stats-label { width: 50%; font-size: 22rpx; color: var(--hai-text-muted); text-align: center; margin-bottom: 12rpx; }
.goal-card { background: var(--hai-card); border-radius: 28rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--hai-shadow); }
.goal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.goal-title { font-size: 28rpx; font-weight: 600; color: var(--hai-text); }
.goal-action { font-size: 24rpx; color: var(--hai-primary); }
.goal-progress-row { display: flex; align-items: baseline; gap: 8rpx; margin-bottom: 16rpx; }
.goal-num { font-family: 'Songti SC', 'STSong', serif; font-size: 40rpx; font-weight: 700; color: var(--hai-text); }
.goal-sep { font-size: 28rpx; color: var(--hai-text-muted); }
.goal-unit { font-size: 24rpx; color: var(--hai-text-muted); margin-left: 4rpx; }
.goal-bar { height: 12rpx; background: var(--hai-border, #eee); border-radius: 999rpx; overflow: hidden; margin-bottom: 12rpx; }
.goal-bar-inner { height: 100%; background: var(--hai-primary); border-radius: 999rpx; }
.goal-tip { font-size: 22rpx; color: var(--hai-text-muted); }
.tab-bar { display: flex; background: var(--hai-card); border-radius: 28rpx; margin-bottom: 16rpx; overflow: hidden; box-shadow: var(--hai-shadow); }
.tab { flex: 1; text-align: center; padding: 22rpx; font-size: 28rpx; color: var(--hai-text-secondary); }
.tab.active { color: var(--hai-primary); font-weight: 600; border-bottom: 4rpx solid var(--hai-primary); }
.tab.link { flex: 0.7; color: var(--hai-primary); font-size: 26rpx; border-bottom: none; }
.calendar-wrap { background: var(--hai-card); border-radius: 28rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: var(--hai-shadow); }
.month-bar { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 12rpx 0 20rpx; }
.nav { font-size: 36rpx; color: var(--hai-primary); }
.month { font-family: 'Songti SC', 'STSong', serif; font-size: 30rpx; font-weight: 700; color: var(--hai-text); }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.w { font-size: 22rpx; color: var(--hai-text-muted); }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; }
.cell { aspect-ratio: 1; min-height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; font-size: 24rpx; color: var(--hai-text); }
.cell.empty { visibility: hidden; }
.cell.studied { background: var(--hai-success-soft); color: var(--hai-success); font-weight: 600; }
.cal-tip { display: block; text-align: center; font-size: 22rpx; color: var(--hai-text-muted); margin-top: 16rpx; }
.material-item { display: flex; align-items: center; padding: 28rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 16rpx; gap: 16rpx; box-shadow: var(--hai-shadow); }
.mat-icon { font-size: 32rpx; }
.mat-info { flex: 1; }
.mat-title { font-size: 28rpx; display: block; color: var(--hai-text); }
.mat-cat { font-size: 22rpx; color: var(--hai-text-muted); display: block; }
.load-more-wrap { padding: 24rpx 0 40rpx; text-align: center; }
.load-more-btn { display: inline-block; padding: 16rpx 48rpx; font-size: 26rpx; color: var(--hai-primary); background: var(--hai-card); border-radius: 999rpx; box-shadow: var(--hai-shadow); }
.load-more-tip { font-size: 24rpx; color: var(--hai-text-muted); }
</style>
