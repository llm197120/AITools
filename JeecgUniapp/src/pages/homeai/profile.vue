<route lang="json5">
{
  style: {
    navigationBarTitleText: '个人中心',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <image class="avatar" :src="userStore.userInfo?.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="user-info">
        <text class="nickname">{{ userStore.isLogin ? (userStore.userInfo?.nickname || '微信用户') : '未登录' }}</text>
        <text class="phone" v-if="userStore.isLogin && userStore.userInfo?.phone">{{ userStore.userInfo.phone }}</text>
        <text class="guest-tip" v-if="!userStore.isLogin">登录后可使用全部功能</text>
      </view>
      <view class="login-btn" v-if="!userStore.isLogin" @click="handleLogin">
        <text>登录</text>
      </view>
    </view>

    <!-- 统计概览 -->
    <view class="stats-card" v-if="userStore.isLogin">
      <view class="stat-item" v-for="s in stats" :key="s.label">
        <text class="stat-num">{{ s.count }}</text>
        <text class="stat-label">{{ s.label }}</text>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-group" v-if="userStore.isLogin">
      <view class="menu-item" @click="goFamily">
        <view class="menu-icon">
          <wd-icon name="home" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">我的家庭</text>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
      <view class="menu-item" @click="showPrivacy">
        <view class="menu-icon">
          <wd-icon name="secured" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <text class="menu-text">隐私协议</text>
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
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { billApi } from '../../pages-homeai/api/bill'
import { learnApi } from '../../pages-homeai/api/learn'
import { get as getApi } from '../../pages-homeai/api/request'
import { localMonthStr } from '../../pages-homeai/utils/date'
import { wechatLogin } from '../../pages-homeai/utils/homeaiAuth'

const userStore = useUserStore()
const loginLoading = ref(false)

const stats = ref([
  { label: '对话次数', count: 0 },
  { label: '文件数', count: 0 },
  { label: '账单数', count: 0 },
])

// 统计短 TTL 缓存：避免频繁切 Tab 重复拉取 3 个统计接口
const STATS_TTL = 30 * 1000
let lastStatsAt = 0

async function loadStats() {
  try {
    const learnStats: any = await learnApi.statistics()
    const month = localMonthStr()
    const entries = (await billApi.entries(month)) || []
    const convs = (await getApi('/ai/conversations/mine')) || []
    stats.value = [
      { label: '对话次数', count: Array.isArray(convs) ? convs.length : 0 },
      { label: '学习次数', count: learnStats?.totalRecords ?? 0 },
      { label: '账单数', count: entries.length },
    ]
    lastStatsAt = Date.now()
  } catch {
    // 统计加载失败不影响页面
  }
}

onShow(async () => {
  if (!userStore.isLogin) {
    stats.value = [
      { label: '对话次数', count: 0 },
      { label: '学习次数', count: 0 },
      { label: '账单数', count: 0 },
    ]
    return
  }
  await userStore.refreshUserInfo()
  if (Date.now() - lastStatsAt < STATS_TTL && stats.value.some((s) => s.count > 0)) {
    return
  }
  await loadStats()
})

function goFamily() {
  if (!userStore.isLogin) return
  uni.switchTab({ url: '/pages/homeai/family' })
}

async function handleLogin() {
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

function showPrivacy() {
  uni.showModal({
    title: '隐私协议',
    content: '我们将严格遵守相关法律法规，保护您的个人信息安全。',
  })
}

function showAbout() {
  uni.showModal({
    title: '关于',
    content: '家庭AI小工具 v1.0\n基于 JeecgBoot + JeecgUniapp 构建',
  })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.switchTab({ url: '/pages/homeai/profile' })
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
.guest-tip {
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  margin-top: 8rpx;
}

.login-btn {
  margin-left: auto;
  padding: 14rpx 32rpx;
  background: var(--hai-primary);
  border-radius: 999rpx;
  color: var(--hai-on-primary);
  font-size: 26rpx;
  font-weight: 600;
  flex-shrink: 0;
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
</style>
