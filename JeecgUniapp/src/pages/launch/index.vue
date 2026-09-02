<route lang="json5">
{
  type: 'home',
  style: {
    navigationStyle: 'custom',
    navigationBarTitleText: '家庭AI小工具',
  },
}
</route>

<template>
  <view class="launch-page">
    <view class="brand-logo">
      <wd-icon name="home" size="44px" color="#FFFFFF"></wd-icon>
    </view>
    <text class="brand-title">家庭AI小工具</text>
  </view>

  <wd-popup
    v-model="updateVisible"
    position="center"
    :close-on-click-modal="false"
    custom-style="width:80%;border-radius:28rpx;overflow:hidden"
  >
    <view class="dialog-title">{{ updateTitle }}</view>
    <view class="dialog-body">
      <text class="dialog-hint">{{ updateLog }}</text>
    </view>
    <view class="dialog-footer">
      <wd-button v-if="!updateForce" block @click="skipUpdate">稍后</wd-button>
      <wd-button type="primary" block @click="acceptUpdate">立即更新</wd-button>
    </view>
  </wd-popup>

  <view v-if="statusText" class="update-mask">
    <text class="dialog-hint">{{ statusText }}</text>
    <view v-if="progress > 0 && progress < 100" class="progress-wrap">
      <view class="progress-bar" :style="{ width: progress + '%' }"></view>
    </view>
    <text v-if="progress > 0" class="dialog-hint">{{ progress }}%</text>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { HOMEAI_LOGIN_PAGE, HOMEAI_PROFILE_TAB, usesPhoneLogin } from '../../pages-homeai/utils/homeaiAuth'
import { probeAndCacheAppBaseUrl } from '../../pages-homeai/platform/env'
import { isStandaloneApp } from '../../pages-homeai/platform/runtime'
import { checkAndApplyUpdate } from '../../pages-homeai/platform/updater'

const updateVisible = ref(false)
const updateForce = ref(false)
const updateTitle = ref('发现新版本')
const updateLog = ref('')
const statusText = ref('')
const progress = ref(0)
let confirmResolve: ((ok: boolean) => void) | null = null

function goNext() {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    uni.switchTab({ url: '/pages/homeai/index' })
    return
  }
  if (usesPhoneLogin()) {
    uni.reLaunch({ url: HOMEAI_LOGIN_PAGE })
    return
  }
  uni.switchTab({ url: HOMEAI_PROFILE_TAB })
}

async function proceedAfterPrivacy() {
  const result = await checkAndApplyUpdate({
    onStatus: (text) => {
      statusText.value = text
    },
    onProgress: (loaded, total) => {
      if (total > 0) {
        progress.value = Math.min(99, Math.round((loaded / total) * 100))
      }
    },
    confirm: (info) =>
      new Promise((resolve) => {
        updateTitle.value = info.versionName ? `发现新版本 ${info.versionName}` : '发现新版本'
        updateLog.value = info.changelog || '有新版本可用'
        updateForce.value = info.force
        updateVisible.value = true
        confirmResolve = resolve
      }),
  })
  if (result === 'continue') {
    goNext()
  }
}

function acceptUpdate() {
  updateVisible.value = false
  confirmResolve?.(true)
  confirmResolve = null
}

function skipUpdate() {
  updateVisible.value = false
  confirmResolve?.(false)
  confirmResolve = null
}

function ensurePrivacyThenGo() {
  // 隐私协议统一在登录页确认，这里不再弹窗（避免打开即弹 + 登录页二次弹出）
  proceedAfterPrivacy()
}

onLoad(async () => {
  await probeAndCacheAppBaseUrl()
  ensurePrivacyThenGo()
})
</script>

<style scoped>
.launch-page {
  min-height: 100vh;
  background: var(--hai-bg, #f3f2ee);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.brand-logo {
  width: 128rpx;
  height: 128rpx;
  border-radius: 36rpx;
  background: var(--hai-primary, #1b4f8a);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-title {
  margin-top: 28rpx;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--hai-text, #3a342c);
}
.dialog-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text, #3a342c);
}
.dialog-body {
  padding: 12rpx 30rpx 20rpx;
}
.dialog-hint {
  display: block;
  font-size: 26rpx;
  color: var(--hai-text-secondary, #8a857c);
  line-height: 1.6;
}
.dialog-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  margin: 8rpx 0;
}
.tip-link {
  font-size: 26rpx;
  color: var(--hai-primary, #1b4f8a);
  text-decoration: underline;
}
.dialog-footer {
  display: flex;
  gap: 20rpx;
  padding: 0 30rpx 30rpx;
}
.update-mask {
  position: fixed;
  inset: 0;
  background: rgba(243, 242, 238, 0.92);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  z-index: 99;
  gap: 16rpx;
}
.progress-wrap {
  width: 70%;
  height: 12rpx;
  border-radius: 999rpx;
  background: rgba(139, 128, 110, 0.2);
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  border-radius: 999rpx;
  background: var(--hai-primary, #1b4f8a);
  transition: width 0.3s ease;
}
</style>
