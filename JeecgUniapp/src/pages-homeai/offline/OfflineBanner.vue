<template>
  <view v-if="showBanner" class="offline-banner">
    <text class="offline-text">{{ text }}</text>
    <text v-if="pending > 0" class="offline-sync">{{ pending }} 条待同步</text>
  </view>
</template>

<script lang="ts" setup>
/**
 * 离线/同步横幅：监听连接状态与同步队列进度。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getConnState, onConnChange } from './conn'
import { getPendingCount, onSyncProgress } from './syncQueue'

const offline = ref(getConnState() === 'offline')
const pending = ref(getPendingCount())
const showBanner = ref(false)

const text = computed(() =>
  offline.value ? '离线模式，展示本地缓存数据' : pending.value > 0 ? '正在同步本地变更…' : '',
)

let offConn: (() => void) | null = null
let offSync: (() => void) | null = null

function refresh() {
  const off = getConnState() === 'offline'
  const p = getPendingCount()
  offline.value = off
  pending.value = p
  showBanner.value = off || p > 0
}

onMounted(() => {
  refresh()
  offConn = onConnChange(refresh)
  offSync = onSyncProgress(refresh)
})

onUnmounted(() => {
  offConn?.()
  offSync?.()
})
</script>

<style scoped>
.offline-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 28rpx;
  background: rgba(139, 128, 110, 0.14);
  border-bottom: 1rpx solid rgba(139, 128, 110, 0.2);
}
.offline-text {
  font-size: 24rpx;
  color: #6b6355;
}
.offline-sync {
  font-size: 22rpx;
  color: #8a857c;
}
</style>
