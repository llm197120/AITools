<route lang="json5">
{
  type: 'home',
  layout: 'default',
  style: {
    navigationStyle: 'custom',
    navigationBarTitleText: '家庭AI小工具',
    disableScroll: true,
  },
}
</route>

<template>
  <view class="homeai-page">
    <!-- 顶部用户信息区域 -->
    <view class="header">
      <view class="user-info" @click="goProfile">
        <image class="avatar" :src="userStore.userInfo?.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
        <view class="info">
          <text class="nickname">{{ userStore.isLogin ? (userStore.userInfo?.nickname || '微信用户') : '未登录' }}</text>
          <text class="family-name" v-if="userStore.isLogin && familyStore.hasFamily">
            {{ familyStore.familyInfo?.name || '我的家庭' }}
          </text>
          <text class="family-name" v-else-if="userStore.isLogin" @click.stop="goFamily">加入家庭 ></text>
          <text class="family-name" v-else @click.stop="goProfile">点击登录 ></text>
        </view>
      </view>
    </view>

    <!-- 今日待办 -->
    <view class="today-todo" v-if="todayTodo > 0" @click="goModule('plan')">
      <wd-icon name="bell" size="18px"></wd-icon>
      <text class="todo-text">今日有 {{ todayTodo }} 项待办计划</text>
      <wd-icon name="arrow-right" size="14px"></wd-icon>
    </view>

    <!-- 九宫格功能入口 -->
    <view class="grid-container">
      <view class="grid-item" v-for="item in modules" :key="item.key" @click="goModule(item.key)">
        <view class="grid-icon" :style="{ backgroundColor: item.bgColor }">
          <text class="icon-text">{{ item.icon }}</text>
        </view>
        <text class="grid-label">{{ item.label }}</text>
      </view>
    </view>

    <!-- 使用条款提示 -->
    <view class="footer-tip">
      <text class="tip-text">使用即表示同意《用户服务协议》</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { get as getApi } from '../../pages-homeai/api/request'
import { ensureLoginForAction, ensureProfileWhenGuest } from '../../pages-homeai/utils/homeaiAuth'

const userStore = useUserStore()
const familyStore = useFamilyStore()
const todayTodo = ref(0)

const modules = [
  { key: 'ai', icon: '🤖', label: 'AI对话', bgColor: '#667eea' },
  { key: 'storage', icon: '📁', label: '资料存储', bgColor: '#f093fb' },
  { key: 'bill', icon: '💰', label: '账单', bgColor: '#4facfe' },
  { key: 'plan', icon: '📋', label: '日常计划', bgColor: '#43e97b' },
  { key: 'recipe', icon: '🍳', label: '烹饪指南', bgColor: '#fa709a' },
  { key: 'learn', icon: '📚', label: '学习模块', bgColor: '#a18cd1' },
  { key: 'more', icon: '➕', label: '更多', bgColor: '#bdc3c7' },
]

onShow(async () => {
  if (!ensureProfileWhenGuest()) {
    return
  }
  await familyStore.fetchFamilyInfo()
  // 今日待办数量
  try {
    const now = new Date()
    const d = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const list = await getApi(`/plan/date/${d}`)
    todayTodo.value = Array.isArray(list) ? list.filter((p: any) => p.status === 'pending').length : 0
  } catch (e) {
    todayTodo.value = 0
  }
})

function goProfile() {
  uni.switchTab({ url: '/pages/homeai/profile' })
}

function goFamily() {
  if (!ensureLoginForAction()) return
  uni.switchTab({ url: '/pages/homeai/family' })
}

function goModule(key: string) {
  if (!ensureLoginForAction()) return
  const pages: Record<string, string> = {
    ai: '/pages-homeai-ai/ai/conversations',
    storage: '/pages-homeai-more/storage/index',
    bill: '/pages-homeai-more/bill/index',
    plan: '/pages-homeai-more/plan/index',
    recipe: '/pages-homeai-more/recipe/index',
    learn: '/pages-homeai-more/learn/index',
    more: '/pages-homeai-more/all-functions/index',
  }
  const url = pages[key]
  if (url) {
    uni.navigateTo({ url })
  } else {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}
</script>

<style scoped>
.homeai-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
}
.header {
  padding: 60rpx 30rpx 30rpx;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}
.info {
  display: flex;
  flex-direction: column;
}
.nickname {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
}
.family-name {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}
.today-todo {
  margin: 0 30rpx 20rpx;
  padding: 20rpx 30rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: #fff;
  font-size: 26rpx;
}
.todo-text {
  flex: 1;
}
.grid-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  padding: 30rpx;
  margin: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}
.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 0;
}
.grid-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}
.icon-text {
  font-size: 44rpx;
}
.grid-label {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}
.footer-tip {
  text-align: center;
  padding: 30rpx;
}
.tip-text {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
}
</style>
