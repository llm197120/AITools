<route lang="json5">
{
  style: {
    navigationBarTitleText: '资料存储',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
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
      <view class="recycle-entry hai-press" @click="goRecycle">
        <wd-icon name="delete" size="18px" color="#1B4F8A" />
        <text>回收站</text>
      </view>
    </view>
    <StorageBrowser ref="browserRef" :folder-id="null" :user-id="userId" />
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import StorageBrowser from './StorageBrowser.vue'

useHomeaiPageGuard()

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id)
const browserRef = ref<{ refresh: () => void; loadMore: () => void } | null>(null)

onShow(async () => {
  preloadWhitelist()
  await userStore.refreshUserInfo()
  browserRef.value?.refresh?.()
})

onReachBottom(() => {
  browserRef.value?.loadMore?.()
})

function goSearch() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/search' })
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
</style>
