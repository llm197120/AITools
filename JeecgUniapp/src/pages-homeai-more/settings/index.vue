<route lang="json5">
{
  style: {
    navigationBarTitleText: '设置',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="hai-page">
    <!-- 外观 -->
    <view class="hai-card section">
      <view class="section-title">外观</view>
      <view class="mode-row" v-for="m in modeOptions" :key="m.value" @click="selectMode(m.value)">
        <text class="mode-label">{{ m.label }}</text>
        <view class="mode-check" :class="{ on: mode === m.value }">
          <text v-if="mode === m.value" class="mode-tick">✓</text>
        </view>
      </view>
    </view>

    <!-- 服务器状态 -->
    <view class="hai-card section">
      <view class="section-title">服务器状态</view>
      <view class="server-row" @click="recheck">
        <text class="server-label">当前状态</text>
        <text class="server-status" :class="'st-' + connState">{{ serverStatusText }}</text>
      </view>
      <text class="server-hint">点击立即重测</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getThemeMode, setThemeMode, applyTheme, ThemeMode } from '../../pages-homeai/utils/theme'
import { getConnState, onConnChange, checkNow } from '../../pages-homeai/offline/conn'

const mode = ref<ThemeMode>(getThemeMode())
const modeOptions = [
  { value: 'system' as ThemeMode, label: '跟随系统' },
  { value: 'light' as ThemeMode, label: '白天模式' },
  { value: 'dark' as ThemeMode, label: '夜晚模式' },
]

function selectMode(m: ThemeMode) {
  mode.value = m
  setThemeMode(m)
  uni.showToast({ title: '已切换', icon: 'none' })
}

// 服务器状态
const connState = ref<'online' | 'offline' | 'unknown'>(getConnState())
const serverStatusText = computed(() => {
  if (connState.value === 'online') return '可用'
  if (connState.value === 'offline') return '不可用'
  return '检测中'
})
let offConn: (() => void) | null = null
onMounted(() => {
  offConn = onConnChange((s) => {
    connState.value = s
  })
  checkNow()
})
onUnmounted(() => {
  offConn?.()
})

async function recheck() {
  const s = await checkNow()
  uni.showToast({
    title: s === 'online' ? '服务器状态：可用' : '服务器状态：不可用',
    icon: 'none',
  })
}
</script>

<style scoped>
.section {
  margin-bottom: 24rpx;
  padding: 24rpx 28rpx;
}
.section-title {
  font-family: var(--hai-serif);
  font-size: 30rpx;
  font-weight: 700;
  color: var(--hai-text);
  margin-bottom: 20rpx;
}
.mode-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid var(--hai-border);
}
.mode-row:last-child {
  border-bottom: none;
}
.mode-label {
  font-size: 28rpx;
  color: var(--hai-text);
}
.mode-check {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid var(--hai-text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mode-check.on {
  background: var(--hai-primary);
  border-color: var(--hai-primary);
}
.mode-tick {
  color: #fff;
  font-size: 22rpx;
}
.server-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
}
.server-label {
  font-size: 28rpx;
  color: var(--hai-text);
}
.server-status {
  font-size: 26rpx;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
}
.server-status.st-online {
  color: #1a8f4b;
  background: rgba(26, 143, 75, 0.12);
}
.server-status.st-offline {
  color: var(--hai-danger);
  background: var(--hai-danger-soft);
}
.server-status.st-unknown {
  color: var(--hai-text-muted);
  background: rgba(138, 133, 124, 0.12);
}
.server-hint {
  display: block;
  font-size: 22rpx;
  color: var(--hai-text-muted);
}
</style>