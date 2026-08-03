<route lang="json5">
{ style: { navigationBarTitleText: '处理历史' } }
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
    <view v-if="tasks.length === 0" class="empty"><text>暂无处理记录</text></view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onShow } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'

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
.history-page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; }
.history-item { display: flex; align-items: center; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; gap: 16rpx; }
.task-icon { font-size: 32rpx; width: 60rpx; text-align: center; }
.task-info { flex: 1; }
.task-name { font-size: 28rpx; color: #333; display: block; }
.task-detail { font-size: 22rpx; color: #999; }
.task-time { font-size: 20rpx; color: #bbb; }
.s-ok { color: #27ae60; } .s-info { color: #2980b9; } .s-warn { color: #f39c12; } .s-err { color: #e74c3c; }
.empty { text-align: center; padding: 100rpx 0; color: #999; }
</style>
