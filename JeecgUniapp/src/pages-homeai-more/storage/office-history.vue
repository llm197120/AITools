<route lang="json5">
{ style: { navigationBarTitleText: '处理历史', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="history-page">
    <view class="history-item" v-for="task in tasks" :key="task.id">
      <view class="task-icon">{{ task.convertType === 'format_convert' ? '🔄' : '🤖' }}</view>
      <view class="task-info">
        <text class="task-name">{{ task.convertType === 'format_convert' ? '格式转换' : 'AI生成' }}</text>
        <text class="task-detail" v-if="task.sourceFormat">{{ task.sourceFormat }} → {{ task.targetFormat }}</text>
        <text class="task-time">{{ formatTime(task.createTime) }}</text>
      </view>
      <view class="task-status">
        <text class="status-text" :class="statusClass(task.status)">{{ statusLabel(task.status) }}</text>
      </view>
    </view>
    <HomeEmpty v-if="tasks.length === 0" title="暂无处理记录" hint="格式转换或 AI 生成后会出现在这里" />
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
import HomeEmpty from '../../components/HomeEmpty.vue'

const tasks = ref<any[]>([])

onShow(async () => { tasks.value = await getApi('/storage/office/history') })

function formatTime(t: string) { return t ? t.substring(0, 16).replace('T', ' ') : '' }

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
.s-ok { color: var(--hai-success); } .s-info { color: var(--hai-primary); } .s-warn { color: #c9a227; } .s-err { color: var(--hai-danger); }
</style>
