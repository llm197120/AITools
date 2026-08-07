<template>
  <view class="home-skeleton" :class="variant">
    <view v-if="variant === 'list'" class="sk-list">
      <view v-for="i in rows" :key="i" class="sk-row">
        <view class="sk-block sk-icon"></view>
        <view class="sk-col">
          <view class="sk-block sk-line long"></view>
          <view class="sk-block sk-line short"></view>
        </view>
      </view>
    </view>
    <view v-else-if="variant === 'card'" class="sk-cards">
      <view v-for="i in 3" :key="i" class="sk-block sk-card"></view>
    </view>
    <view v-else class="sk-grid">
      <view v-for="i in 7" :key="i" class="sk-block sk-cell"></view>
    </view>
  </view>
</template>
<script lang="ts" setup>
withDefaults(
  defineProps<{ variant?: 'list' | 'card' | 'calendar'; rows?: number }>(),
  { variant: 'list', rows: 4 },
)
</script>
<style scoped>
.home-skeleton { padding: 20rpx; }
.sk-row { display: flex; gap: 16rpx; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; }
.sk-icon { width: 48rpx; height: 48rpx; border-radius: 8rpx; flex-shrink: 0; }
.sk-col { flex: 1; }
.sk-line { height: 24rpx; border-radius: 6rpx; margin-bottom: 12rpx; }
.sk-line.long { width: 70%; }
.sk-line.short { width: 40%; margin-bottom: 0; }
.sk-cards { display: flex; gap: 16rpx; }
.sk-card { flex: 1; height: 120rpx; border-radius: 12rpx; }
.sk-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; background: #fff; padding: 16rpx; border-radius: 16rpx; }
.sk-cell { aspect-ratio: 1; border-radius: 8rpx; }
.sk-block {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
