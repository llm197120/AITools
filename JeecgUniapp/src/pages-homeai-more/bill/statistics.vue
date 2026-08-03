<route lang="json5">
{ style: { navigationBarTitleText: '统计分析' } }
</route>

<template>
  <view class="page">
    <view class="month-select">
      <text @click="changeMonth(-1)">◀</text>
      <text class="month">{{ currentMonth }}</text>
      <text @click="changeMonth(1)">▶</text>
    </view>
    <view class="stats">
      <view class="stat-item"><text class="s-label">总支出</text><text class="s-value red">¥{{ stats.expense }}</text></view>
      <view class="stat-item"><text class="s-label">总收入</text><text class="s-value green">¥{{ stats.income }}</text></view>
      <view class="stat-item"><text class="s-label">笔数</text><text class="s-value">{{ stats.count }}</text></view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
const currentMonth = ref(new Date().toISOString().substring(0,7))
const stats = ref({expense:'0',income:'0',count:0})
onShow(() => loadStats())
async function loadStats() { stats.value = await getApi('/bill/statistics', {params:{month:currentMonth.value}}) }
function changeMonth(d:number) {
  const [y,m] = currentMonth.value.split('-').map(Number)
  const dt = new Date(y,m-1+d); currentMonth.value = dt.toISOString().substring(0,7)
  loadStats()
}
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.month-select{display:flex;justify-content:center;align-items:center;gap:40rpx;padding:30rpx;font-size:32rpx;font-weight:600;color:#333}
.stats{display:flex;gap:16rpx}
.stat-item{flex:1;background:#fff;border-radius:12rpx;padding:30rpx;text-align:center}
.s-label{font-size:22rpx;color:#999}.s-value{font-size:40rpx;font-weight:700;display:block;margin-top:8rpx}.red{color:#e74c3c}.green{color:#27ae60}
</style>
