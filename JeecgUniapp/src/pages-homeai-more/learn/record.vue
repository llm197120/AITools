<route lang="json5">{ style: { navigationBarTitleText: '学习记录', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true } }</route>
<template>
  <view class="hai-page">
    <view class="month-bar">
      <text class="nav" @click="prevMonth">‹</text>
      <text class="month">{{ viewYear }}年{{ viewMonth }}月</text>
      <text class="nav" @click="nextMonth">›</text>
      <text class="manual-btn" @click="manualVisible = true">+ 补记</text>
    </view>
    <view class="week-head">
      <text v-for="w in weekLabels" :key="w" class="w">{{ w }}</text>
    </view>
    <view class="grid">
      <view
        v-for="(cell, i) in calendarCells"
        :key="i"
        class="cell"
        :class="{ empty: !cell.date, studied: cell.studied, selected: cell.date === selectedDate, today: cell.isToday }"
        @click="cell.date && selectDate(cell.date)"
      >
        <text v-if="cell.date">{{ cell.day }}</text>
      </view>
    </view>
    <view class="records">
      <view v-if="loading"><HomeSkeleton variant="list" :rows="3" /></view>
      <HomeEmpty
        v-else-if="loadFailed"
        title="记录加载失败"
        hint="请检查网络后重试"
        action-text="重试"
        @action="loadData"
      />
      <template v-else>
        <view v-if="selectedDate" class="list-head">
          <text>{{ selectedDate }} · {{ displayedRecords.length }} 条</text>
          <text class="clear-day" @click="selectedDate = ''">看全月</text>
        </view>
        <view class="record" v-for="r in displayedRecords" :key="r.id">
          <text class="title">{{ r.materialTitle || r.title || '学习记录' }}</text>
          <text class="meta">{{ formatDuration(r.duration) }} · {{ formatDate(r.startTime || r.createTime) }}</text>
          <text v-if="r.notes" class="notes">{{ r.notes }}</text>
        </view>
        <HomeEmpty v-if="displayedRecords.length === 0" :title="emptyTitle" />
      </template>
    </view>

    <wd-popup
      v-model="manualVisible"
      position="center"
      custom-style="width:80%;border-radius:28rpx;overflow:hidden"
    >
      <view class="dialog-title">手动补记学习</view>
      <view class="dialog-body">
        <text class="dialog-hint">
          补记所选日期（{{ selectedDate || '今日' }}）的学习时长，联网后自动同步。
        </text>
        <view class="manual-row">
          <text class="manual-label">时长（分钟）</text>
          <wd-input v-model="manualMinutes" type="number" placeholder="如 30" />
        </view>
      </view>
      <view class="dialog-footer">
        <wd-button block @click="manualVisible = false">取消</wd-button>
        <wd-button type="primary" block :loading="manualSaving" @click="saveManual">保存</wd-button>
      </view>
    </wd-popup>
  </view>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { learnApi } from '../../pages-homeai/api/learn'
import { mutate } from '../../pages-homeai/offline/dataAccess'
import { addMonths, localDateStr, toDateStr, toDateTimeStr } from '../../pages-homeai/utils/date'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'

useHomeaiPageGuard()
useHomeaiPullRefresh(() => loadData())

const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth() + 1)
const studyDates = ref<string[]>([])
const records = ref<any[]>([])
const selectedDate = ref('')
const loading = ref(true)
const loadFailed = ref(false)
const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const todayStr = localDateStr()

const yearMonthParam = computed(() => `${viewYear.value}-${String(viewMonth.value).padStart(2, '0')}`)
const isCurrentMonth = computed(() => yearMonthParam.value === todayStr.slice(0, 7))
const displayedRecords = computed(() => records.value)
const emptyTitle = computed(() => {
  if (selectedDate.value) return `${selectedDate.value} 暂无学习记录`
  return isCurrentMonth.value ? '本月暂无学习记录' : `${viewYear.value}年${viewMonth.value}月暂无学习记录`
})

const calendarCells = computed(() => {
  const first = new Date(viewYear.value, viewMonth.value - 1, 1)
  const lastDay = new Date(viewYear.value, viewMonth.value, 0).getDate()
  const startWeek = first.getDay()
  const cells: any[] = []
  for (let i = 0; i < startWeek; i++) cells.push({})
  for (let d = 1; d <= lastDay; d++) {
    const date = `${yearMonthParam.value}-${String(d).padStart(2, '0')}`
    cells.push({ date, day: d, studied: studyDates.value.includes(date), isToday: date === todayStr })
  }
  return cells
})

function formatDuration(sec: number) {
  const m = Math.floor((sec || 0) / 60)
  return m > 0 ? `${m} 分钟` : `${sec || 0} 秒`
}

function formatDate(t: string) {
  return toDateTimeStr(t)
}

let loadSeq = 0

async function loadData(silent = false) {
  const seq = ++loadSeq
  if (!silent) loading.value = true
  loadFailed.value = false
  try {
    const dates = ((await learnApi.calendar(yearMonthParam.value)) || []).map(toDateStr).filter(Boolean)
    const list =
      (await learnApi.records({
        yearMonth: yearMonthParam.value,
        studyDate: selectedDate.value || undefined,
      })) || []
    if (seq !== loadSeq) return
    studyDates.value = dates
    records.value = list
  } catch {
    if (seq !== loadSeq) return
    loadFailed.value = records.value.length === 0
    if (!silent) {
      records.value = []
      studyDates.value = []
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function selectDate(date: string) {
  selectedDate.value = selectedDate.value === date ? '' : date
  loadData(records.value.length > 0)
}

// 手动补记（离线可用：入队，联网后缓慢同步）
const manualVisible = ref(false)
const manualMinutes = ref('30')
const manualSaving = ref(false)

async function saveManual() {
  const minutes = Number(manualMinutes.value)
  if (!Number.isFinite(minutes) || minutes <= 0 || minutes > 1440) {
    uni.showToast({ title: '请输入 1-1440 的分钟数', icon: 'none' })
    return
  }
  if (manualSaving.value) return
  manualSaving.value = true
  try {
    const res = await mutate(
      'learn',
      'addRecord',
      { materialId: '', duration: minutes, recordType: 'manual', notes: '手动补记' },
      (p) => learnApi.addRecord(p.materialId, p.duration, p.notes),
    )
    manualVisible.value = false
    uni.showToast({
      title: res.queued ? '已离线保存，联网后同步' : '已记录',
      icon: res.queued ? 'none' : 'success',
    })
    await loadData(true)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    manualSaving.value = false
  }
}

function shiftMonth(delta: number) {
  const next = addMonths(yearMonthParam.value, delta)
  viewYear.value = Number(next.slice(0, 4))
  viewMonth.value = Number(next.slice(5, 7))
  selectedDate.value = ''
  records.value = []
  studyDates.value = []
  loading.value = true
  loadData()
}

function prevMonth() {
  shiftMonth(-1)
}

function nextMonth() {
  shiftMonth(1)
}

onLoad((opts: any) => {
  const date = String(opts?.date || '')
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    viewYear.value = Number(date.slice(0, 4))
    viewMonth.value = Number(date.slice(5, 7))
    selectedDate.value = date
  }
})

onShow(() => loadData(records.value.length > 0))
</script>
<style scoped>
/* page shell: .hai-page */
.month-bar { display: flex; justify-content: center; align-items: center; gap: 40rpx; padding: 12rpx 0 20rpx; }
.nav { font-size: 36rpx; color: var(--hai-primary); }
.month { font-size: 30rpx; font-weight: 600; color: var(--hai-text); }
.manual-btn {
  font-size: 26rpx;
  color: var(--hai-primary);
}
.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text);
}
.dialog-body { padding: 12rpx 30rpx 20rpx; }
.dialog-hint { display: block; font-size: 24rpx; color: var(--hai-text-secondary); line-height: 1.5; margin-bottom: 16rpx; }
.manual-row { display: flex; align-items: center; gap: 16rpx; }
.manual-label { font-size: 26rpx; color: var(--hai-text); flex-shrink: 0; }
.dialog-footer { display: flex; gap: 20rpx; padding: 0 30rpx 30rpx; }
.week-head { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; margin-bottom: 8rpx; }
.w { font-size: 22rpx; color: var(--hai-text-muted); }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: var(--hai-card); border-radius: 28rpx; padding: 16rpx; margin-bottom: 20rpx; box-shadow: var(--hai-shadow); }
.cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 8rpx; font-size: 24rpx; color: var(--hai-text); }
.cell.empty { visibility: hidden; }
.cell.studied { background: var(--hai-success-soft); color: var(--hai-success); font-weight: 600; }
.cell.today:not(.selected) { border: 2rpx solid var(--hai-primary); }
.cell.selected { background: var(--hai-primary); color: var(--hai-on-primary); font-weight: 600; }
.records { background: var(--hai-card); border-radius: 28rpx; padding: 16rpx; box-shadow: var(--hai-shadow); }
.list-head { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 8rpx 12rpx; font-size: 24rpx; color: var(--hai-text-secondary); }
.clear-day { color: var(--hai-primary); }
.record { padding: 20rpx 8rpx; border-bottom: 1rpx solid var(--hai-border); }
.title { font-size: 28rpx; color: var(--hai-text); display: block; }
.meta { font-size: 22rpx; color: var(--hai-text-muted); display: block; margin-top: 6rpx; }
.notes { font-size: 24rpx; color: var(--hai-text-secondary); display: block; margin-top: 8rpx; }
</style>
