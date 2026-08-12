<route lang="json5">
{
  style: {
    navigationBarTitleText: '全部功能',
    navigationBarBackgroundColor: '#F3F2EE',
  },
}
</route>

<template>
  <view class="hai-page">
    <view class="group">
      <text class="group-title">功能模块</text>
      <view class="item" v-for="m in modules" :key="m.path" @click="go(m.path)">
        <view class="item-icon">
          <wd-icon :name="m.icon" size="18px" :color="haiPrimary" />
        </view>
        <view class="item-texts">
          <text class="label">{{ m.label }}</text>
          <text class="sub">{{ m.sub }}</text>
        </view>
        <wd-icon name="arrow-right" size="14px" :color="haiTertiary" />
      </view>
    </view>
    <view class="group">
      <text class="group-title">快捷操作</text>
      <view class="item" @click="go('/pages-homeai-more/bill/add?type=expense')">
        <view class="item-icon">
          <wd-icon name="money-circle" size="18px" :color="haiPrimary" />
        </view>
        <view class="item-texts">
          <text class="label">记一笔支出</text>
          <text class="sub">快速记账</text>
        </view>
        <wd-icon name="arrow-right" size="14px" :color="haiTertiary" />
      </view>
      <view class="item" @click="go('/pages-homeai-more/bill/import')">
        <view class="item-icon">
          <wd-icon name="file-add" size="18px" :color="haiPrimary" />
        </view>
        <view class="item-texts">
          <text class="label">导入账单</text>
          <text class="sub">识别账单明细</text>
        </view>
        <wd-icon name="arrow-right" size="14px" :color="haiTertiary" />
      </view>
      <view class="item" @click="go('/pages-homeai-more/learn/record')">
        <view class="item-icon">
          <wd-icon name="history" size="18px" :color="haiPrimary" />
        </view>
        <view class="item-texts">
          <text class="label">学习记录</text>
          <text class="sub">查看打卡历史</text>
        </view>
        <wd-icon name="arrow-right" size="14px" :color="haiTertiary" />
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
/** 与 --hai-primary / --hai-text-tertiary 对齐（组件 color 属性需实色） */
const haiPrimary = '#1B4F8A'
const haiTertiary = '#C4BFB6'

const modules = [
  { icon: 'chat', label: 'AI对话', sub: '智能问答助手', path: '/pages-homeai-ai/ai/conversations' },
  { icon: 'folder', label: '资料存储', sub: '文件与 Office', path: '/pages-homeai-more/storage/index' },
  { icon: 'money-circle', label: '账单管理', sub: '收支与统计', path: '/pages-homeai-more/bill/index' },
  { icon: 'calendar', label: '日常计划', sub: '日程与待办', path: '/pages-homeai-more/plan/index' },
  { icon: 'goods', label: '烹饪指南', sub: '菜谱灵感', path: '/pages-homeai-more/recipe/index' },
  { icon: 'books', label: '学习模块', sub: '资料与打卡', path: '/pages-homeai-more/learn/index' },
  { icon: 'usergroup', label: '家庭管理', sub: '成员与邀请', path: '/pages/homeai/family' },
  { icon: 'user', label: '个人中心', sub: '账号与设置', path: '/pages/homeai/profile' },
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
/* page shell: .hai-page */
.group {
  background: var(--hai-card);
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  margin-bottom: 24rpx;
  overflow: hidden;
}

.group-title {
  display: block;
  font-size: 22rpx;
  color: var(--hai-text-muted);
  padding: 24rpx 28rpx 8rpx;
}

.item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid var(--hai-border);
}

.item:last-child {
  border-bottom: none;
}

.item-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  background: var(--hai-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-texts {
  flex: 1;
  min-width: 0;
}

.label {
  display: block;
  font-size: 28rpx;
  color: var(--hai-text);
  font-weight: 500;
}

.sub {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: var(--hai-text-muted);
}
</style>
