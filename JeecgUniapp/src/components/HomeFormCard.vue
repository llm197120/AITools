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

/* 分组卡片：一行一项、内部发丝分割，避免多张小卡片挤在一起 */
.home-form-group {
  background: var(--hai-card, #fff);
  border-radius: var(--hai-radius, 28rpx);
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: var(--hai-shadow, 0 8rpx 32rpx rgba(27, 40, 60, 0.06));
}

.home-form-group .wd-cell-group,
.home-form-group .wd-cell-group__body {
  background: transparent !important;
  margin: 0;
}

.home-form-group .wd-cell,
.home-form-group .wd-picker,
.home-form-group .wd-select-picker,
.home-form-group .wd-datetime-picker {
  background: #fff !important;
}

.home-form-group .wd-cell__wrapper {
  padding: 28rpx 24rpx;
  min-height: 96rpx;
  box-sizing: border-box;
}

.home-form-group .wd-cell__title,
.home-form-group .wd-cell__left {
  color: var(--hai-text, #2c271f);
  font-size: 28rpx;
}

.home-form-group .wd-cell__value,
.home-form-group .wd-picker,
.home-form-group .wd-select-picker .wd-cell__value {
  color: var(--hai-text-secondary, #8a857c);
  font-size: 26rpx;
}

.home-form-cell-input {
  flex: 1;
  width: 100%;
  text-align: right;
  font-size: 28rpx;
  color: var(--hai-text, #2c271f);
}

.home-form-save {
  margin-top: 8rpx;
}
</style>
