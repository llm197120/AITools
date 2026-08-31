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
        <text class="edit-hint" v-if="userStore.isLogin">点击编辑昵称与头像</text>
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
      <view class="menu-item" @click="goProfileEdit">
        <view class="menu-icon">
          <wd-icon name="edit" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">编辑资料</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view class="menu-item" v-if="showChangePassword" @click="goChangePassword">
        <view class="menu-icon">
          <wd-icon name="lock-on" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">修改密码</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view class="menu-item" @click="goFamily">
        <view class="menu-icon">
          <wd-icon name="home" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">我的家庭</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view v-if="phoneLoginApp" class="menu-item" @click="showApiBase">
        <view class="menu-icon">
          <wd-icon name="setting" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">服务器地址</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view class="menu-item" @click="showPrivacy">
        <view class="menu-icon">
          <wd-icon name="secured" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">隐私政策</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view class="menu-item" @click="showAbout">
        <view class="menu-icon">
          <wd-icon name="info" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">关于</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" v-if="userStore.isLogin" @click="handleLogout">
      <text>退出登录</text>
    </view>

    <wd-popup
      v-if="phoneLoginApp"
      v-model="apiBaseVisible"
      position="center"
      custom-style="width:80%;border-radius:28rpx;overflow:hidden"
    >
      <view class="dialog-title">服务器地址</view>
      <view class="dialog-body">
        <text class="dialog-hint">内测换网段时填写电脑局域网地址，例如 http://192.168.1.8:8080/jeecg-boot</text>
        <wd-input v-model="apiBaseInput" placeholder="http://主机:8080/jeecg-boot" />
      </view>
      <view class="dialog-footer">
        <wd-button block @click="apiBaseVisible = false">取消</wd-button>
        <wd-button type="primary" block :loading="apiBaseSaving" @click="saveApiBase">保存</wd-button>
      </view>
    </wd-popup>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { billApi } from '../../pages-homeai/api/bill'
import { learnApi } from '../../pages-homeai/api/learn'
import { get as getApi } from '../../pages-homeai/api/request'
import { localMonthStr } from '../../pages-homeai/utils/date'
import { displayNickname } from '../../pages-homeai/utils/displayName'
import { openAuthPage, jumpToGuestAuth, usesPhoneLogin, wechatLogin } from '../../pages-homeai/utils/homeaiAuth'
import { getServerBaseUrl, setAppBaseUrl, pingAppBaseUrl } from '../../pages-homeai/platform/env'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { useFamilyPoll } from '../../pages-homeai/utils/useFamilyPoll'

const userStore = useUserStore()
const familyStore = useFamilyStore()
const loginLoading = ref(false)
const apiBaseVisible = ref(false)
const apiBaseInput = ref('')
const apiBaseSaving = ref(false)
const phoneLoginApp = usesPhoneLogin()

const showChangePassword = computed(() => {
  if (userStore.userInfo?.loginType === 'phone') return true
  return phoneLoginApp
})

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

function showApiBase() {
  apiBaseInput.value = getServerBaseUrl()
  apiBaseVisible.value = true
}

async function saveApiBase() {
  const url = apiBaseInput.value.trim().replace(/\/$/, '')
  if (!/^https?:\/\/.+/i.test(url)) {
    uni.showToast({ title: '请输入 http(s) 地址', icon: 'none' })
    return
  }
  if (apiBaseSaving.value) return
  apiBaseSaving.value = true
  try {
    setAppBaseUrl(url)
    const ok = await pingAppBaseUrl(url)
    apiBaseVisible.value = false
    uni.showToast({
      title: ok ? '已保存，后续请求走新地址' : '已保存，但当前探测未通，请确认电脑与手机同网',
      icon: 'none',
      duration: 2500,
    })
  } finally {
    apiBaseSaving.value = false
  }
}

function showAbout() {
  const sys = uni.getSystemInfoSync()
  const ver = sys.appVersion || sys.appWgtVersion || '1.0.1'
  uni.showModal({
    title: '关于',
    content: `家庭AI小工具 v${ver}\n面向家庭的记账、菜谱、学习与 AI 助手`,
  })
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
</style>
