<template>
  <view class="home-empty" :class="{ card: card }">
    <wd-icon
      v-if="iconName"
      class="wd-icon-wrap"
      :name="iconName"
      :size="iconSize"
      :color="iconColor"
    />
    <text v-else-if="icon" class="icon">{{ icon }}</text>
    <text class="title">{{ title }}</text>
    <text v-if="hint" class="hint">{{ hint }}</text>
    <slot name="actions">
      <view v-if="actionText" class="action" @click="$emit('action')">{{ actionText }}</view>
    </slot>
    <slot />
  </view>
</template>
<script lang="ts" setup>
withDefaults(
  defineProps<{
    /** 兼容旧用法：emoji / 文字图标（优先使用 iconName） */
    icon?: string
    /** wot-design 图标名，推荐 */
    iconName?: string
    iconSize?: string
    iconColor?: string
    title?: string
    hint?: string
    actionText?: string
    /** 是否使用白卡片底（列表页推荐开启） */
    card?: boolean
  }>(),
  {
    icon: '',
    iconName: 'inbox',
    iconSize: '48px',
    iconColor: '#C4BFB6',
    title: '暂无数据',
    hint: '',
    actionText: '',
    card: true,
  },
)
defineEmits<{ action: [] }>()
</script>
<style scoped>
.home-empty {
  text-align: center;
  padding: 64rpx 40rpx;
}
.home-empty.card {
  background: var(--hai-card, #fff);
  border-radius: var(--hai-radius, 28rpx);
  box-shadow: var(--hai-shadow, 0 8rpx 32rpx rgba(27, 40, 60, 0.06));
}
.wd-icon-wrap {
  display: block;
  margin-bottom: 16rpx;
}
.icon {
  font-size: 64rpx;
  display: block;
  margin-bottom: 16rpx;
}
.title {
  font-size: 28rpx;
  color: var(--hai-text-secondary, #8a857c);
  display: block;
}
.hint {
  font-size: 24rpx;
  color: var(--hai-text-muted, #a39e94);
  display: block;
  margin-top: 8rpx;
  line-height: 1.45;
}
.action {
  display: inline-block;
  margin-top: 28rpx;
  padding: 16rpx 40rpx;
  background: var(--hai-primary, #1b4f8a);
  color: var(--hai-on-primary, #fff);
  border-radius: 999rpx;
  font-size: 26rpx;
}
</style>
