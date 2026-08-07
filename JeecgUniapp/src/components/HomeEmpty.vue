<template>
  <view class="home-empty">
    <text class="icon">{{ icon }}</text>
    <text class="title">{{ title }}</text>
    <text v-if="hint" class="hint">{{ hint }}</text>
    <view v-if="actionText" class="action" @click="$emit('action')">{{ actionText }}</view>
  </view>
</template>
<script lang="ts" setup>
withDefaults(
  defineProps<{ icon?: string; title?: string; hint?: string; actionText?: string }>(),
  { icon: '📭', title: '暂无数据', hint: '', actionText: '' },
)
defineEmits<{ action: [] }>()
</script>
<style scoped>
.home-empty { text-align: center; padding: 80rpx 40rpx; }
.icon { font-size: 72rpx; display: block; margin-bottom: 16rpx; }
.title { font-size: 28rpx; color: #666; display: block; }
.hint { font-size: 24rpx; color: #999; display: block; margin-top: 8rpx; }
.action { display: inline-block; margin-top: 24rpx; padding: 16rpx 40rpx; background: #667eea; color: #fff; border-radius: 32rpx; font-size: 26rpx; }
</style>
