<route lang="json5">{ style: { navigationBarTitleText: '全部功能' } }</route>
<template>
  <view class="page">
    <view class="group">
      <text class="group-title">功能模块</text>
      <view class="item" v-for="m in modules" :key="m.path" @click="go(m.path)">
        <text class="icon">{{ m.icon }}</text>
        <text class="label">{{ m.label }}</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc" />
      </view>
    </view>
    <view class="group">
      <text class="group-title">快捷操作</text>
      <view class="item" @click="go('/pages-homeai-more/bill/add?type=expense')">
        <text class="icon">💸</text>
        <text class="label">记一笔支出</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc" />
      </view>
      <view class="item" @click="go('/pages-homeai-more/bill/import')">
        <text class="icon">📥</text>
        <text class="label">导入账单</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc" />
      </view>
      <view class="item" @click="go('/pages-homeai-more/learn/record')">
        <text class="icon">📅</text>
        <text class="label">学习记录</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc" />
      </view>
    </view>
  </view>
</template>
<script lang="ts" setup>
const modules = [
  { icon: '🤖', label: 'AI对话', path: '/pages-homeai-ai/ai/conversations' },
  { icon: '📁', label: '资料存储', path: '/pages-homeai-more/storage/index' },
  { icon: '💰', label: '账单管理', path: '/pages-homeai-more/bill/index' },
  { icon: '📋', label: '日常计划', path: '/pages-homeai-more/plan/index' },
  { icon: '🍳', label: '烹饪指南', path: '/pages-homeai-more/recipe/index' },
  { icon: '📚', label: '学习模块', path: '/pages-homeai-more/learn/index' },
  { icon: '👪', label: '家庭管理', path: '/pages/homeai/family' },
  { icon: '👤', label: '个人中心', path: '/pages/homeai/profile' },
]

function go(path: string) {
  if (path.includes('/pages/homeai/')) {
    uni.switchTab({ url: path })
  } else {
    uni.navigateTo({ url: path })
  }
}
</script>
<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; }
.group { background: #fff; border-radius: 16rpx; margin-bottom: 20rpx; overflow: hidden; }
.group-title { display: block; font-size: 24rpx; color: #999; padding: 20rpx 24rpx 8rpx; }
.item { display: flex; align-items: center; gap: 16rpx; padding: 28rpx 24rpx; border-bottom: 1rpx solid #f0f0f0; }
.item:last-child { border-bottom: none; }
.icon { font-size: 32rpx; width: 40rpx; }
.label { flex: 1; font-size: 28rpx; color: #333; }
</style>
