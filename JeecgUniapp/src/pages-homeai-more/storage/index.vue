<route lang="json5">
{
  style: {
    navigationBarTitleText: '资料存储',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="storage-page">
    <view class="top-actions">
      <view class="search-bar hai-press" @click="goSearch">
        <wd-icon name="search" size="18px" color="#8A857C" />
        <text class="search-placeholder">搜索文件与文件夹</text>
      </view>
      <view class="recycle-entry hai-press" @click="goGenerate">
        <wd-icon name="edit" size="18px" color="#1B4F8A" />
        <text>AI生成</text>
      </view>
      <view class="recycle-entry hai-press" @click="goHistory">
        <wd-icon name="clock" size="18px" color="#1B4F8A" />
        <text>处理记录</text>
      </view>
      <view class="recycle-entry hai-press" @click="goRecycle">
        <wd-icon name="delete" size="18px" color="#1B4F8A" />
        <text>回收站</text>
      </view>
    </view>
    <view v-if="usage" class="usage-card" :class="{ warn: usage.overWarn || usage.familyOverWarn }">
      <view class="usage-row">
        <text class="usage-label">我的空间</text>
        <text class="usage-value">{{ formatSize(usage.usedBytes) }} / {{ formatSize(usage.limitBytes) }}</text>
      </view>
      <view class="usage-bar">
        <view class="usage-fill" :style="{ width: usageBarWidth(usage.usedPercent) }"></view>
      </view>
      <view v-if="usage.familyId" class="usage-row family">
        <text class="usage-label">{{ usage.familyName || '家庭' }}空间</text>
        <text class="usage-value">{{ formatSize(usage.familyUsedBytes) }} / {{ formatSize(usage.familyLimitBytes) }}</text>
      </view>
      <view v-if="usage.familyId" class="usage-bar">
        <view class="usage-fill" :style="{ width: usageBarWidth(usage.familyUsedPercent) }"></view>
      </view>
    </view>
    <StorageBrowser ref="browserRef" :folder-id="null" :user-id="userId" />
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { storageApi } from '../../pages-homeai/api/storage'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import StorageBrowser from './StorageBrowser.vue'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  await userStore.refreshUserInfo()
  await loadUsage()
  browserRef.value?.refresh?.()
})

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id)
const browserRef = ref<{ refresh: (silent?: boolean) => void; loadMore: () => void } | null>(null)
const usage = ref<any>(null)

onShow(async () => {
  preloadWhitelist()
  await userStore.refreshUserInfo()
  await loadUsage()
  browserRef.value?.refresh?.(true)
})

async function loadUsage() {
  try {
    usage.value = await storageApi.myUsage()
  } catch {
    usage.value = null
  }
}

function usageBarWidth(percent?: number) {
  const n = Number(percent)
  if (!Number.isFinite(n) || n <= 0) return '0%'
  return Math.min(100, n) + '%'
}

function formatSize(bytes?: number) {
  const n = Number(bytes) || 0
  if (n < 1024) return n + 'B'
  if (n < 1048576) return (n / 1024).toFixed(1) + 'KB'
  if (n < 1073741824) return (n / 1048576).toFixed(1) + 'MB'
  return (n / 1073741824).toFixed(2) + 'GB'
}

onReachBottom(() => {
  browserRef.value?.loadMore?.()
})

function goSearch() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/search' })
}

function goGenerate() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/office-generate' })
}

function goHistory() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/office-history' })
}

function goRecycle() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/recycle' })
}
</script>

<style scoped>
.storage-page {
  min-height: 100vh;
  background: var(--hai-bg);
}
.top-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 24rpx 32rpx 0;
}
.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 22rpx 28rpx;
  min-height: 88rpx;
  box-sizing: border-box;
  background: var(--hai-card);
  border-radius: 999rpx;
  box-shadow: var(--hai-shadow);
}
.recycle-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  min-width: 96rpx;
  padding: 12rpx 8rpx;
  font-size: 20rpx;
  color: var(--hai-primary, #1b4f8a);
  background: var(--hai-card);
  border-radius: 20rpx;
  box-shadow: var(--hai-shadow);
}
.search-placeholder {
  font-size: 28rpx;
  color: var(--hai-text-muted);
}
.usage-card {
  margin: 16rpx 32rpx 0;
  padding: 20rpx 24rpx;
  background: var(--hai-card);
  border-radius: 20rpx;
  box-shadow: var(--hai-shadow);
}
.usage-card.warn .usage-fill {
  background: #c4564a;
}
.usage-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.usage-row.family {
  margin-top: 16rpx;
}
.usage-label {
  font-size: 24rpx;
  color: var(--hai-text-muted);
}
.usage-value {
  font-size: 24rpx;
  color: var(--hai-text);
}
.usage-bar {
  margin-top: 10rpx;
  height: 10rpx;
  border-radius: 999rpx;
  background: rgba(27, 79, 138, 0.12);
  overflow: hidden;
}
.usage-fill {
  height: 100%;
  border-radius: 999rpx;
  background: var(--hai-primary, #1b4f8a);
}
</style>
