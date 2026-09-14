<route lang="json5">
{
  style: {
    navigationBarTitleText: '个人中心',
    navigationBarBackgroundColor: '#F3F2EE',
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="user-card" @click="goProfileEdit">
      <image class="avatar" :src="userStore.userInfo?.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="user-info">
        <text class="nickname">{{ userStore.isLogin ? displayNickname(userStore.userInfo) : '未登录' }}</text>
        <text class="phone" v-if="userStore.isLogin && userStore.userInfo?.phone">{{ userStore.userInfo.phone }}</text>
        <text class="guest-tip" v-if="!userStore.isLogin">登录或注册后可使用全部功能</text>
        <text class="edit-hint" v-if="userStore.isLogin">点击编辑姓名与头像</text>
      </view>
      <view class="auth-actions" v-if="!userStore.isLogin">
        <view class="login-btn" :class="{ disabled: loginLoading }" @click.stop="handleLogin">
          <text>{{ loginLoading ? '登录中…' : '登录' }}</text>
        </view>
        <view v-if="phoneLoginApp" class="login-btn register-btn" @click.stop="handleRegister">
          <text>注册</text>
        </view>
      </view>
    </view>

    <!-- 统计概览 -->
    <view class="stats-card" v-if="userStore.isLogin">
      <view class="stat-item" v-for="s in stats" :key="s.label" @click="goStat(s.path)">
        <text class="stat-num">{{ s.count }}</text>
        <text class="stat-label">{{ s.label }}</text>
      </view>
    </view>
    <text v-if="userStore.isLogin && statsFailed" class="stats-fail" @click="retryStats">统计加载失败，点此重试</text>

    <!-- 菜单列表 -->
    <view class="menu-group" v-if="userStore.isLogin">
      <view class="menu-item" v-if="showChangePassword" @click="goChangePassword">
        <view class="menu-icon">
          <wd-icon name="lock-on" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">修改密码</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
      <view class="menu-item" @click="goFamily">
        <view class="menu-icon">
          <wd-icon name="home" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">我的家庭</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
      <view v-if="phoneLoginApp" class="menu-item" @click="goSettings">
        <view class="menu-icon">
          <wd-icon name="setting" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">设置</text>
        <text class="server-status" :class="'st-' + connState">{{ serverStatusText }}</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
      <view class="menu-item" @click="goAgreement">
        <view class="menu-icon">
          <wd-icon name="edit" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">用户协议</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
      <view class="menu-item" @click="showPrivacy">
        <view class="menu-icon">
          <wd-icon name="secured" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">隐私政策</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
      <view class="menu-item" @click="showAbout">
        <view class="menu-icon">
          <wd-icon name="info" size="18px" color="var(--hai-primary)"></wd-icon>
        </view>
        <text class="menu-text">关于</text>
        <wd-icon name="arrow-right" size="14px" color="var(--hai-text-tertiary)"></wd-icon>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" v-if="userStore.isLogin" @click="handleLogout">
      <text>退出登录</text>
    </view>

    <wd-popup
      v-model="aboutVisible"
      position="center"
      custom-style="width:80%;border-radius:28rpx;overflow:hidden;background:var(--hai-card)"
    >
      <view class="dialog-title">关于</view>
      <view class="dialog-body">
        <text class="dialog-hint">{{ aboutText }}</text>
      </view>
      <view class="dialog-footer dialog-footer-col">
        <wd-button type="primary" block :loading="checkingUpdate" @click="checkAppUpdate">检查更新</wd-button>
        <wd-button block @click="aboutVisible = false">关闭</wd-button>
      </view>
    </wd-popup>

    <wd-popup
      v-model="updateVisible"
      position="center"
      :close-on-click-modal="!updateForce"
      custom-style="width:80%;border-radius:28rpx;overflow:hidden;background:var(--hai-card)"
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
      <view v-if="downloading" class="progress-wrap">
        <view
          class="progress-bar"
          :class="{ indeterminate: !(progress > 0) }"
          :style="progress > 0 ? { width: progress + '%' } : {}"
        ></view>
      </view>
      <text v-if="progress > 0" class="dialog-hint">{{ progress }}%</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { billApi } from '../../pages-homeai/api/bill'
import { learnApi } from '../../pages-homeai/api/learn'
import { get as getApi } from '../../pages-homeai/api/request'
import { localMonthStr } from '../../pages-homeai/utils/date'
import { displayNickname } from '../../pages-homeai/utils/displayName'
import { openAuthPage, jumpToGuestAuth, usesPhoneLogin, wechatLogin } from '../../pages-homeai/utils/homeaiAuth'
import { isStandaloneApp } from '../../pages-homeai/platform/runtime'
import {
  applyInspectedUpdate,
  getLocalAppVersion,
  inspectAppUpdate,
  type UpdateInspectResult,
} from '../../pages-homeai/platform/updater'
import { applyTheme } from '../../pages-homeai/utils/theme'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { useFamilyPoll } from '../../pages-homeai/utils/useFamilyPoll'
import {
  getConnState,
  onConnChange,
  checkNow,
  pokeConnection,
} from '../../pages-homeai/offline/conn'

const userStore = useUserStore()
const familyStore = useFamilyStore()
const loginLoading = ref(false)
const phoneLoginApp = usesPhoneLogin()

const showChangePassword = computed(() => {
  if (userStore.userInfo?.loginType === 'phone') return true
  return phoneLoginApp
})

// 服务器状态（离线能力）：只显示可用性，不展示/不修改服务器地址
const connState = ref<'online' | 'offline' | 'unknown'>(getConnState())
const serverStatusText = computed(() => {
  if (connState.value === 'online') return '可用'
  if (connState.value === 'offline') return '不可用'
  return '检测中'
})
let offConnChange: (() => void) | null = null
onMounted(() => {
  offConnChange = onConnChange((s) => {
    connState.value = s
  })
})
onUnmounted(() => {
  offConnChange?.()
})

/** 设置页（外观模式 + 服务器状态） */
function goSettings() {
  uni.navigateTo({ url: '/pages-homeai-more/settings/index' })
}

/** 用户协议（我的页面入口） */
function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/index' })
}

const stats = ref([
  { label: '对话', count: 0, path: '/pages-homeai-ai/ai/conversations' },
  { label: '学习', count: 0, path: '/pages-homeai-more/learn/index' },
  { label: '账单', count: 0, path: '/pages-homeai-more/bill/index' },
])
const statsFailed = ref(false)

async function loadStats() {
  const month = localMonthStr()
  const [learnRes, billRes, convRes] = await Promise.allSettled([
    learnApi.statistics(),
    billApi.summary(month),
    getApi('/ai/conversations/mine', { pageNo: '1', pageSize: '1' }),
  ])
  const next = [...stats.value]
  if (learnRes.status === 'fulfilled') {
    next[1] = { ...next[1], count: (learnRes.value as any)?.totalRecords ?? 0 }
  }
  if (billRes.status === 'fulfilled') {
    next[2] = { ...next[2], count: Number((billRes.value as any)?.count ?? 0) }
  }
  if (convRes.status === 'fulfilled') {
    const convs = convRes.value
    next[0] = {
      ...next[0],
      count: Array.isArray(convs) ? convs.length : Number((convs as any)?.total ?? 0),
    }
  }
  stats.value = next
  statsFailed.value = [learnRes, billRes, convRes].every((r) => r.status === 'rejected')
}

function retryStats() {
  loadStats()
}

useHomeaiPullRefresh(async () => {
  if (!userStore.isLogin) return
  await userStore.refreshUserInfo()
  await loadStats()
})

const { start: startFamilyPoll, stop: stopFamilyPoll } = useFamilyPoll()

onShow(async () => {
  applyTheme()
  stopFamilyPoll()
  if (!userStore.isLogin) {
    stats.value = [
      { label: '对话', count: 0, path: '/pages-homeai-ai/ai/conversations' },
      { label: '学习', count: 0, path: '/pages-homeai-more/learn/index' },
      { label: '账单', count: 0, path: '/pages-homeai-more/bill/index' },
    ]
    return
  }
  await userStore.refreshUserInfo()
  await familyStore.fetchFamilyInfo()
  await loadStats()
  startFamilyPoll()
})

function goFamily() {
  if (!userStore.isLogin) return
  uni.switchTab({ url: '/pages/homeai/family' })
}

function goProfileEdit() {
  if (!userStore.isLogin) return
  uni.navigateTo({ url: '/pages/auth/profile-edit' })
}

function goChangePassword() {
  if (!userStore.isLogin) return
  uni.navigateTo({ url: '/pages/auth/change-password' })
}

function goStat(path?: string) {
  if (!path) return
  uni.navigateTo({ url: path })
}

async function handleLogin() {
  if (phoneLoginApp) {
    openAuthPage()
    return
  }
  if (loginLoading.value) return
  loginLoading.value = true
  try {
    await wechatLogin()
    uni.showToast({ title: '登录成功', icon: 'success' })
    await userStore.refreshUserInfo()
    await loadStats()
  } catch (e) {
    console.error('登录失败', e)
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  } finally {
    loginLoading.value = false
  }
}

function handleRegister() {
  openAuthPage('register')
}

function showPrivacy() {
  uni.navigateTo({ url: '/pages/privacy/index' })
}

const aboutVisible = ref(false)
const aboutText = ref('')
const checkingUpdate = ref(false)
const updateVisible = ref(false)
const updateForce = ref(false)
const updateTitle = ref('发现新版本')
const updateLog = ref('')
const statusText = ref('')
const progress = ref(0)
const downloading = computed(() => statusText.value.includes('下载'))
let pendingUpdate: Extract<UpdateInspectResult, { kind: 'available' }> | null = null

async function showAbout() {
  const local = await getLocalAppVersion()
  const build = local.build ? ` (${local.build})` : ''
  aboutText.value = `家庭AI小工具 v${local.versionName}${build}\n面向家庭的记账、菜谱、学习与 AI 助手`
  aboutVisible.value = true
}

async function checkAppUpdate() {
  if (checkingUpdate.value) return
  if (!isStandaloneApp()) {
    uni.showToast({ title: '仅安装版可检查更新', icon: 'none' })
    return
  }
  checkingUpdate.value = true
  try {
    const inspected = await inspectAppUpdate()
    if (inspected.kind === 'offline') {
      uni.showToast({ title: '无法连接服务器', icon: 'none' })
      return
    }
    if (inspected.kind === 'skip') {
      uni.showToast({ title: '当前环境无法检查更新', icon: 'none' })
      return
    }
    if (inspected.kind === 'latest') {
      uni.showToast({ title: '已是最新版本', icon: 'success' })
      return
    }
    pendingUpdate = inspected
    updateTitle.value = inspected.remote.versionName
      ? `发现新版本 ${inspected.remote.versionName}`
      : '发现新版本'
    updateLog.value = inspected.remote.changelog || '有新版本可用'
    updateForce.value = inspected.remote.forceUpdate === true
    aboutVisible.value = false
    updateVisible.value = true
  } finally {
    checkingUpdate.value = false
  }
}

async function acceptUpdate() {
  if (!pendingUpdate) return
  updateVisible.value = false
  const inspected = pendingUpdate
  pendingUpdate = null
  const result = await applyInspectedUpdate(inspected, {
    onStatus: (text) => {
      statusText.value = text
    },
    onProgress: (loaded, total) => {
      if (total > 0) progress.value = Math.min(99, Math.round((loaded / total) * 100))
    },
  })
  if (result === 'failed') statusText.value = ''
}

function skipUpdate() {
  updateVisible.value = false
  pendingUpdate = null
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        await userStore.logout()
        jumpToGuestAuth()
      }
    },
  })
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 32rpx 48rpx;
  background: var(--hai-bg);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 32rpx;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: var(--hai-bg);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.nickname {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 36rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.phone,
.guest-tip,
.edit-hint {
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  margin-top: 8rpx;
}

.auth-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.login-btn {
  padding: 14rpx 32rpx;
  background: var(--hai-primary);
  border-radius: 999rpx;
  color: var(--hai-on-primary);
  font-size: 26rpx;
  font-weight: 600;
}
.login-btn.disabled {
  opacity: 0.6;
}

.register-btn {
  background: transparent;
  color: var(--hai-primary);
  border: 2rpx solid var(--hai-primary);
}

.stats-fail {
  display: block;
  margin-top: 12rpx;
  padding: 0 8rpx;
  font-size: 24rpx;
  color: var(--hai-danger);
}
.stats-card {
  display: flex;
  margin-top: 24rpx;
  padding: 32rpx 16rpx;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
}

.stat-item {
  flex: 1;
  text-align: center;
  position: relative;
}

.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 10rpx;
  bottom: 10rpx;
  width: 1rpx;
  background: var(--hai-border);
}

.stat-num {
  display: block;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 40rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.stat-label {
  font-size: 22rpx;
  color: var(--hai-text-muted);
  margin-top: 8rpx;
  display: block;
}

.menu-group {
  margin-top: 24rpx;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  gap: 16rpx;
  border-bottom: 1rpx solid var(--hai-border);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  background: var(--hai-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: var(--hai-text);
}

.server-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}
.server-status.st-online {
  color: #1a8f4b;
  background: rgba(26, 143, 75, 0.1);
}
.server-status.st-offline {
  color: var(--hai-danger, #c0392b);
  background: rgba(192, 57, 43, 0.1);
}
.server-status.st-unknown {
  color: var(--hai-text-muted);
  background: rgba(138, 133, 124, 0.12);
}

.retest-btn {
  margin-top: 12rpx;
  padding: 16rpx;
  text-align: center;
  border-radius: 999rpx;
  background: var(--hai-primary-soft, rgba(27, 79, 138, 0.1));
  color: var(--hai-primary, #1b4f8a);
  font-size: 26rpx;
}

.logout-btn {
  margin-top: 40rpx;
  padding: 28rpx;
  text-align: center;
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  color: var(--hai-danger);
  font-size: 28rpx;
}

.dialog-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text);
}
.dialog-body { padding: 12rpx 30rpx 20rpx; }
.dialog-hint {
  display: block;
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  line-height: 1.5;
  margin-bottom: 16rpx;
}
.dialog-footer { display: flex; gap: 20rpx; padding: 0 30rpx 30rpx; }
.dialog-footer-col { flex-direction: column; }
.update-mask {
  position: fixed;
  inset: 0;
  background: rgba(23, 24, 28, 0.72);
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
  background: var(--hai-border);
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  background: var(--hai-primary);
  border-radius: 999rpx;
}
.progress-bar.indeterminate {
  width: 36%;
  animation: hai-progress-slide 1.1s ease-in-out infinite;
}
@keyframes hai-progress-slide {
  0% { transform: translateX(-120%); }
  100% { transform: translateX(280%); }
}
</style>
