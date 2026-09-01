<route lang="json5">
{
  style: {
    navigationBarTitleText: '学习模块',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="hai-page hai-page--fab">
    <OfflineBanner />
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
      <text class="goal-tip">{{ goal.reached ? '今日目标已达成' : remindHint }}</text>
    </view>

    <view v-if="activeSession.materialId" class="learn-bar">
      <view class="learn-bar-info">
        <text class="learn-bar-title">正在学习：{{ activeSession.materialTitle }}</text>
        <text class="learn-bar-time">{{ formatElapsed(displayElapsed) }}</text>
      </view>
      <view class="learn-bar-stop" @click="stopLearn">结束并保存</view>
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
        <view
          v-for="(cell, i) in calendarCells"
          :key="i"
          class="cell"
          :class="{ empty: !cell.date, studied: cell.studied }"
          @click="cell.date && goRecordDay(cell.date)"
        >
          <text v-if="cell.date">{{ cell.day }}</text>
        </view>
      </view>
      <text class="cal-tip">点日期查看当天学习记录</text>
    </view>

    <template v-else>
      <view class="search-bar">
        <input class="search-input" v-model="keyword" placeholder="搜索资料标题" confirm-type="search" @confirm="searchMaterials" />
        <text v-if="keyword" class="search-clear" @click="clearSearch">清除</text>
      </view>
      <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>
      <HomeEmpty
        v-else-if="loadFailed"
        title="资料加载失败"
        hint="请检查网络后重试"
        action-text="重试"
        @action="keyword.trim() ? searchMaterials() : retryMaterials()"
      />
      <template v-else>
        <view class="material-item" v-for="m in materials" :key="m.id" @click="openDetail(m)">
          <view class="mat-icon">{{ getTypeIcon(m.type) }}</view>
          <view class="mat-info">
            <text class="mat-title">{{ m.title }}</text>
            <text class="mat-cat">{{ m.category }} · {{ m.type }}</text>
          </view>
          <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
        </view>
        <HomeEmpty
          v-if="materials.length === 0 && !loadingMore"
          :title="keyword.trim() ? '未找到相关资料' : '暂无学习资料'"
          :hint="keyword.trim() ? '换个词试试，或清空搜索' : '添加资料后即可开始计时学习'"
          :action-text="keyword.trim() ? '清空搜索' : '添加资料'"
          @action="keyword.trim() ? clearSearch() : goAdd()"
        />
      </template>
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
    <wd-action-sheet
      v-model="goalSheetVisible"
      :actions="goalActions"
      cancel-text="取消"
      @select="onGoalSelect"
    />
  </view>
</template>

<script lang="ts" setup>
import { computed, ref, onUnmounted } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { learnApi, configApi } from '../../pages-homeai/api/index'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { scheduleLearnGoalRemind } from '../../pages-homeai/utils/push'
import { isStandaloneApp } from '../../pages-homeai/platform/runtime'
import { readList, mutate } from '../../pages-homeai/offline/dataAccess'
import OfflineBanner from '../../pages-homeai/offline/OfflineBanner.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { confirmStopLearn } from '../../pages-homeai/utils/learnSession'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  await loadData(true)
  if (activeTab.value === 'calendar') await loadCalendar()
})

const remindHint = isStandaloneApp()
  ? '每晚 20:00 未达标将发本地通知（进程被杀时可能收不到）'
  : '每晚 20:00 未达标将提醒（需授权订阅）'

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
const loading = ref(false)
const loadingMore = ref(false)
const loadFailed = ref(false)
const keyword = ref('')
let timerHandle: ReturnType<typeof setInterval> | null = null
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const GOAL_OPTIONS = [15, 30, 45, 60, 90]
const goalSheetVisible = ref(false)
const goalActions = GOAL_OPTIONS.map((m) => ({ name: `每日 ${m} 分钟` }))

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
function retryMaterials() {
  fetchMaterials(true)
}

function searchMaterials() {
  fetchMaterials(true)
}

function clearSearch() {
  keyword.value = ''
  fetchMaterials(true)
}

async function fetchMaterials(reset = false, silent = false) {
  if (loadingMore.value || loading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    loading.value = !silent && materials.value.length === 0
    loadFailed.value = false
  } else {
    loadingMore.value = true
  }
  try {
    const nextPage = reset ? 1 : pageNo.value + 1
    let page: any
    if (reset) {
      const res = await readList<any>(
        'learn',
        'materials:' + (keyword.value.trim() || 'all'),
        () => learnApi.materials(1, PAGE_SIZE, undefined, keyword.value),
      )
      page = res.data
      if (res.offline) {
        uni.showToast({ title: '离线模式，展示本地数据', icon: 'none' })
      }
    } else {
      page = await learnApi.materials(nextPage, PAGE_SIZE, undefined, keyword.value)
    }
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
  } catch {
    if (reset) {
      loadFailed.value = materials.value.length === 0
      if (loadFailed.value) materials.value = []
      else uni.showToast({ title: '刷新失败', icon: 'none' })
    } else {
      uni.showToast({ title: '加载更多失败', icon: 'none' })
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadGoal() {
  try {
    goal.value = (await learnApi.goal()) || goal.value
    scheduleLearnGoalRemind(goal.value)
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
  } catch {
    // 订阅失败不阻断
  }
}

function editGoal() {
  goalSheetVisible.value = true
}

async function onGoalSelect({ index }: { index: number }) {
  const minutes = GOAL_OPTIONS[index]
  if (!minutes) return
  await requestLearnSubscribe()
  const res = await mutate(
    'learn',
    'setGoal',
    { minutes },
    (p) => learnApi.setGoal(p.minutes),
  )
  goal.value = minutes
  scheduleLearnGoalRemind(goal.value)
  lastStatsAt = 0
  await loadData(true)
  uni.showToast({
    title: res.queued ? '目标已离线保存，联网后同步' : '目标已更新',
    icon: res.queued ? 'none' : 'success',
  })
}

// 统计/目标短 TTL 缓存：避免频繁返回本页重复拉取（列表与进行中会话保持实时）
const LEARN_STATS_TTL = 30 * 1000
let lastStatsAt = 0

async function loadData(forceStats = false, silent = false) {
  pageNo.value = 1
  hasMore.value = true
  await fetchMaterials(true, silent)
  if (forceStats || Date.now() - lastStatsAt >= LEARN_STATS_TTL) {
    try {
      const statsRes = await readList<any>('learn', 'stats', () => learnApi.statistics())
      stats.value = statsRes.data || {}
      await loadGoal()
      lastStatsAt = Date.now()
    } catch {
      // 统计/目标失败不阻断资料列表与计时条
    }
  }
  await loadActiveSession()
}

async function loadMore() {
  if (activeTab.value !== 'list') return
  await fetchMaterials(false)
}

async function loadCalendar() {
  try {
    studyDates.value = (await learnApi.calendar(yearMonthParam.value)) || []
  } catch {
    uni.showToast({ title: '日历加载失败', icon: 'none' })
  }
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

function openDetail(m: any) {
  uni.navigateTo({ url: `/pages-homeai-more/learn/detail?id=${m.id}` })
}

const stopping = ref(false)

async function stopLearn() {
  if (!activeSession.value.materialId || stopping.value) return
  if (!(await confirmStopLearn())) return
  stopping.value = true
  try {
    const res = await mutate(
      'learn',
      'stop',
      { materialId: activeSession.value.materialId },
      (p) => learnApi.stop(p.materialId),
    )
    uni.showToast({
      title: res.queued ? '已离线记录，联网后同步' : '已记录学习时长',
      icon: res.queued ? 'none' : 'success',
    })
    activeSession.value = {}
    clearTimer()
    await loadData(true)
  } finally {
    stopping.value = false
  }
}

function goAdd() {
  uni.navigateTo({ url: '/pages-homeai-more/learn/add' })
}

function goRecord() {
  uni.navigateTo({ url: '/pages-homeai-more/learn/record' })
}

function goRecordDay(date: string) {
  uni.navigateTo({ url: `/pages-homeai-more/learn/record?date=${date}` })
}

onShow(() => {
  preloadWhitelist()
  loadData(false, materials.value.length > 0)
})

onReachBottom(() => {
  loadMore()
})

onUnmounted(() => clearTimer())
</script>

<style scoped>
/* page shell: .hai-page */
.search-bar{position:relative;padding:8rpx 0 16rpx}
.search-input{height:72rpx;padding:0 88rpx 0 28rpx;background:var(--hai-card);border-radius:999rpx;font-size:28rpx;box-shadow:var(--hai-shadow)}
.search-clear{position:absolute;right:28rpx;top:50%;transform:translateY(-50%);font-size:24rpx;color:var(--hai-primary)}
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
