<template>
  <view class="home-form">
    <slot />
  </view>
</template>

<script lang="ts" setup>
// 表单页统一暖灰底 + 内边距容器
</script>

<style scoped>
.home-form {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 32rpx 48rpx;
  background: var(--hai-bg, #f3f2ee);
  color: var(--hai-text, #2c271f);
}
</style>

<style>
/* 非 scoped：供表单页直接复用的字段样式 */
.home-form-input {
  width: 100%;
  font-size: 32rpx;
  padding: 24rpx;
  background: var(--hai-card, #fff);
  border-radius: var(--hai-radius, 28rpx);
  margin-bottom: 20rpx;
  box-sizing: border-box;
  color: var(--hai-text, #2c271f);
  box-shadow: var(--hai-shadow, 0 8rpx 32rpx rgba(27, 40, 60, 0.06));
}

.home-form-textarea {
  width: 100%;
  min-height: 150rpx;
  font-size: 28rpx;
  padding: 24rpx;
  background: var(--hai-card, #fff);
  border-radius: var(--hai-radius, 28rpx);
  margin-bottom: 20rpx;
  box-sizing: border-box;
  color: var(--hai-text, #2c271f);
  box-shadow: var(--hai-shadow, 0 8rpx 32rpx rgba(27, 40, 60, 0.06));
}

.home-form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 24rpx;
  background: var(--hai-card, #fff);
  border-radius: var(--hai-radius-md, 24rpx);
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: var(--hai-text, #2c271f);
  box-shadow: var(--hai-shadow, 0 8rpx 32rpx rgba(27, 40, 60, 0.06));
}

.home-form-row .home-form-value {
  color: var(--hai-text-secondary, #8a857c);
  max-width: 360rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 26rpx;
}
</style>
