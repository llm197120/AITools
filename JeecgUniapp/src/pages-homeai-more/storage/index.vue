<route lang="json5">

{ style: { navigationBarTitleText: '资料存储' } }

</route>



<template>

  <view class="storage-page">

    <view class="header-bar">

      <wd-icon name="search" size="20px" @click="goSearch"></wd-icon>

      <text class="header-title">我的资料库</text>

      <view style="width: 20px"></view>

    </view>

    <StorageBrowser ref="browserRef" :folder-id="null" :user-id="userId" />

  </view>

</template>



<script lang="ts" setup>

import { ref, computed } from 'vue'

import { onShow } from '@dcloudio/uni-app'

import { useUserStore } from '../../pages-homeai/stores/user'

import { preloadWhitelist } from '../../pages-homeai/utils/fileWhitelist'

import StorageBrowser from './StorageBrowser.vue'



const userStore = useUserStore()

const userId = computed(() => userStore.userInfo?.id)

const browserRef = ref<{ refresh: () => void } | null>(null)



onShow(async () => {

  preloadWhitelist()

  await userStore.refreshUserInfo()

  browserRef.value?.refresh?.()

})



function goSearch() {

  uni.navigateTo({ url: '/pages-homeai-more/storage/search' })

}

</script>



<style scoped>

.storage-page { min-height: 100vh; background: #f5f5f5; }

.header-bar { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 30rpx; background: #fff; }

.header-title { font-size: 32rpx; font-weight: 600; }

</style>

