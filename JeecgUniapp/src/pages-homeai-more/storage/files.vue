<route lang="json5">
{
  style: {
    navigationBarTitleText: '文件夹',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <StorageBrowser ref="browserRef" :folder-id="folderId" :user-id="userId" />
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'
import StorageBrowser from './StorageBrowser.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  await userStore.refreshUserInfo()
  browserRef.value?.refresh?.()
})

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.id)
const folderId = ref<string | null>(null)
const browserRef = ref<{ refresh: (silent?: boolean) => void; loadMore: () => void } | null>(null)

onLoad((options: any) => {
  folderId.value = options?.folderId || null
  if (options?.name) {
    uni.setNavigationBarTitle({ title: decodeURIComponent(options.name) })
  }
})

onShow(async () => {
  preloadWhitelist()
  await userStore.refreshUserInfo()
  browserRef.value?.refresh?.(true)
})

onReachBottom(() => {
  browserRef.value?.loadMore?.()
})
</script>
