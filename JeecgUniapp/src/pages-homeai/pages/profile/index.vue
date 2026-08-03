<route lang="json5">
{
  style: {
    navigationBarTitleText: '个人中心',
  },
}
</route>

<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <image class="avatar" :src="userStore.userInfo?.avatarUrl || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="user-info">
        <text class="nickname">{{ userStore.userInfo?.nickname || '微信用户' }}</text>
        <text class="phone" v-if="userStore.userInfo?.phone">{{ userStore.userInfo.phone }}</text>
      </view>
    </view>

    <!-- 统计概览 -->
    <view class="stats-card">
      <view class="stat-item" v-for="s in stats" :key="s.label">
        <text class="stat-num">{{ s.count }}</text>
        <text class="stat-label">{{ s.label }}</text>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-group">
      <view class="menu-item" @click="goFamily">
        <wd-icon name="home" size="20px"></wd-icon>
        <text class="menu-text">我的家庭</text>
        <wd-icon name="arrow-right" size="14px" class="menu-arrow"></wd-icon>
      </view>
      <view class="menu-item" @click="showPrivacy">
        <wd-icon name="info" size="20px"></wd-icon>
        <text class="menu-text">隐私协议</text>
        <wd-icon name="arrow-right" size="14px" class="menu-arrow"></wd-icon>
      </view>
      <view class="menu-item" @click="showAbout">
        <wd-icon name="help" size="20px"></wd-icon>
        <text class="menu-text">关于</text>
        <wd-icon name="arrow-right" size="14px" class="menu-arrow"></wd-icon>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="handleLogout">
      <text>退出登录</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const stats = ref([
  { label: '对话次数', count: 0 },
  { label: '文件数', count: 0 },
  { label: '账单数', count: 0 },
])

onShow(async () => {
  await userStore.refreshUserInfo()
})

function goFamily() {
  uni.navigateTo({ url: '/pages/homeai/pages/family/index' })
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
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },
  })
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 30rpx;
  padding: 60rpx 30rpx;
  background: linear-gradient(135deg, #667eea, #764ba2);
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}
.user-info {
  display: flex;
  flex-direction: column;
}
.nickname {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
}
.phone {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 8rpx;
}
.stats-card {
  display: flex;
  margin: -40rpx 20rpx 0;
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
}
.stat-item {
  flex: 1;
  text-align: center;
}
.stat-num {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}
.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}
.menu-group {
  margin: 30rpx 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 30rpx;
  gap: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}
.menu-arrow {
  color: #ccc;
}
.logout-btn {
  margin: 60rpx 20rpx;
  padding: 28rpx;
  text-align: center;
  background: #fff;
  border-radius: 16rpx;
  color: #e74c3c;
  font-size: 28rpx;
}
</style>
