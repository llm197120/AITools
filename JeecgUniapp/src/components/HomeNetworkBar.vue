<template>
  <view v-if="visible" class="network-bar" :class="type">
    <text>{{ message }}</text>
  </view>
</template>
<script lang="ts" setup>
import { onMounted, onUnmounted, ref } from 'vue'

const visible = ref(false)
const type = ref<'offline' | 'online'>('offline')
const message = ref('')

let hideTimer: ReturnType<typeof setTimeout> | null = null

function onNetworkChange(res: UniNamespace.OnNetworkStatusChangeSuccess) {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  if (!res.isConnected) {
    type.value = 'offline'
    message.value = '网络连接已断开，部分功能不可用'
    visible.value = true
  } else {
    type.value = 'online'
    message.value = '网络已恢复'
    visible.value = true
    hideTimer = setTimeout(() => {
      visible.value = false
    }, 3000)
  }
}

onMounted(() => {
  uni.getNetworkType({
    success: (res) => {
      if (res.networkType === 'none') onNetworkChange({ isConnected: false, networkType: 'none' })
    },
  })
  uni.onNetworkStatusChange(onNetworkChange)
})

onUnmounted(() => {
  uni.offNetworkStatusChange(onNetworkChange)
  if (hideTimer) clearTimeout(hideTimer)
})
</script>
<style scoped>
.network-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  padding: 16rpx 24rpx;
  text-align: center;
  font-size: 24rpx;
  color: #fff;
}
.network-bar.offline { background: #e74c3c; }
.network-bar.online { background: #27ae60; }
</style>
