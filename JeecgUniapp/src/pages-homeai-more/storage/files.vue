<route lang="json5">

{ style: { navigationBarTitleText: '文件夹' } }

</route>



<template>

  <StorageBrowser ref="browserRef" :folder-id="folderId" :user-id="userId" />

</template>



<script lang="ts" setup>

import { ref, computed } from 'vue'

import { onLoad, onShow } from '@dcloudio/uni-app'

import { useUserStore } from '../../pages-homeai/stores/user'

import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'

import StorageBrowser from './StorageBrowser.vue'



const userStore = useUserStore()

const userId = computed(() => userStore.userInfo?.id)

const folderId = ref<string | null>(null)

const browserRef = ref<{ refresh: () => void } | null>(null)



onLoad((options: any) => {

  folderId.value = options?.folderId || null

  if (options?.name) {

    uni.setNavigationBarTitle({ title: decodeURIComponent(options.name) })

  }

})



onShow(async () => {

  preloadWhitelist()

  await userStore.refreshUserInfo()

  browserRef.value?.refresh?.()

})

</script>

