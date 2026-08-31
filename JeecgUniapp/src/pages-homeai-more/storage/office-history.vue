<route lang="json5">
{ style: { navigationBarTitleText: '处理历史', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true, onReachBottomDistance: 80 } }
</route>

<template>
  <view class="history-page">
    <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>
    <HomeEmpty
      v-else-if="loadFailed"
      title="记录加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      @action="() => loadHistory()"
    />
    <template v-else>
      <view class="history-item" v-for="task in tasks" :key="task.id" @click="openResult(task)">
        <view class="task-icon">{{ task.convertType === 'format_convert' ? '🔄' : '🤖' }}</view>
        <view class="task-info">
          <text class="task-name">{{ task.convertType === 'format_convert' ? '格式转换' : 'AI生成' }}</text>
          <text class="task-detail" v-if="task.sourceFormat">{{ task.sourceFormat }} → {{ task.targetFormat }}</text>
          <text class="task-time">{{ formatTime(task.createTime) }}</text>
        </view>
        <view class="task-status">
          <text class="status-text" :class="statusClass(task.status)">{{ statusLabel(task.status) }}</text>
          <text v-if="task.status === 'COMPLETED'" class="open-hint">打开</text>
          <text v-else-if="task.status === 'FAILED'" class="open-hint">查看原因</text>
        </view>
      </view>
      <HomeEmpty v-if="tasks.length === 0" title="暂无处理记录" hint="格式转换或 AI 生成后会出现在这里" />
      <view v-if="tasks.length > 0" class="load-more-wrap">
        <view v-if="loadingMore" class="load-more-tip">加载中...</view>
        <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
        <view v-else class="load-more-tip">没有更多了</view>
      </view>
    </template>
  </view>
</template>

<script lang="ts" setup>
import { onUnmounted, ref } from 'vue'
import { onHide, onShow, onReachBottom } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
import { downloadStorageFile } from '../../pages-homeai/utils/fileDownload'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { toDateTimeStr } from '../../pages-homeai/utils/date'

useHomeaiPageGuard()
useHomeaiPullRefresh(() => loadHistory(false, true))

const tasks = ref<any[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const PAGE_SIZE = 20
const pageNo = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

function hasActiveTask() {
  return tasks.value.some((t) => t.status === 'PENDING' || t.status === 'PROCESSING')
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function schedulePoll() {
  stopPoll()
  if (!hasActiveTask()) return
  pollTimer = setInterval(() => {
    loadHistory(true, false)
  }, 3000)
}

onShow(() => loadHistory(false, true))
onReachBottom(() => loadMore())
onHide(stopPoll)
onUnmounted(stopPoll)

function loadMore() {
  loadHistory(false, false)
}

function mergeFirstPage(incoming: any[]) {
  const ids = new Set(incoming.map((t) => t.id))
  tasks.value = [...incoming, ...tasks.value.filter((t) => !ids.has(t.id))]
}

async function loadHistory(silent = false, reset = true) {
  if (!reset && !silent && (loadingMore.value || !hasMore.value)) return
  if (reset && !silent) loading.value = tasks.value.length === 0
  if (!reset && !silent) loadingMore.value = true
  if (!silent) loadFailed.value = false
  try {
    const nextPage = reset || silent ? 1 : pageNo.value + 1
    const res: any = await getApi('/storage/office/history', {
      pageNo: String(nextPage),
      pageSize: String(PAGE_SIZE),
    })
    const records: any[] = Array.isArray(res) ? res : (res?.records || [])
    if (Array.isArray(res)) {
      tasks.value = records
      hasMore.value = false
    } else if (silent) {
      mergeFirstPage(records)
    } else if (reset) {
      tasks.value = records
      pageNo.value = 1
      const total = res?.total
      hasMore.value = typeof total === 'number' ? tasks.value.length < total : records.length >= PAGE_SIZE
    } else {
      tasks.value = tasks.value.concat(records)
      pageNo.value = nextPage
      const total = res?.total
      hasMore.value = typeof total === 'number' ? tasks.value.length < total : records.length >= PAGE_SIZE
    }
  } catch {
    if (tasks.value.length === 0) {
      loadFailed.value = true
      tasks.value = []
    } else if (!silent) {
      uni.showToast({ title: '记录刷新失败', icon: 'none' })
    }
  } finally {
    loading.value = false
    loadingMore.value = false
    schedulePoll()
  }
}

async function openResult(task: any) {
  if (task.status === 'FAILED') {
    uni.showToast({ title: task.errorMessage || '处理失败', icon: 'none' })
    return
  }
  if (task.status !== 'COMPLETED') {
    uni.showToast({ title: statusLabel(task.status), icon: 'none' })
    return
  }
  if (!task.resultFileUrl) {
    uni.showToast({ title: '暂无结果文件', icon: 'none' })
    return
  }
  await downloadStorageFile({
    fileUrl: task.resultFileUrl,
    originalName: `office-${task.targetFormat || 'result'}`,
    extension: task.targetFormat,
  })
}

function formatTime(t: string) {
  return toDateTimeStr(t)
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '等待中', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }
  return map[s] || s
}

function statusClass(s: string) {
  return { PENDING: 's-warn', PROCESSING: 's-info', COMPLETED: 's-ok', FAILED: 's-err' }[s] || ''
}
</script>

<style scoped>
.history-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 48rpx; box-sizing: border-box; }
.history-item { display: flex; align-items: center; padding: 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; gap: 16rpx; box-shadow: var(--hai-shadow); }
.task-icon { font-size: 32rpx; width: 60rpx; text-align: center; }
.task-info { flex: 1; }
.task-name { font-size: 28rpx; color: var(--hai-text); display: block; }
.task-detail { font-size: 22rpx; color: var(--hai-text-muted); }
.task-time { font-size: 20rpx; color: var(--hai-text-muted); }
.task-status { display: flex; flex-direction: column; align-items: flex-end; gap: 6rpx; }
.open-hint { font-size: 22rpx; color: var(--hai-primary); }
.s-ok { color: var(--hai-success); } .s-info { color: var(--hai-primary); } .s-warn { color: #c9a227; } .s-err { color: var(--hai-danger); }
.load-more-wrap { width: 100%; padding: 16rpx 0 40rpx; text-align: center; }
.load-more-btn { display: inline-block; padding: 16rpx 48rpx; font-size: 26rpx; color: var(--hai-primary); background: var(--hai-card); border-radius: 999rpx; box-shadow: var(--hai-shadow); }
.load-more-tip { font-size: 24rpx; color: var(--hai-text-muted); }
</style>
