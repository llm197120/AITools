<route lang="json5">
{ style: { navigationBarTitleText: '菜谱详情' } }
</route>

<template>
  <view class="page">
    <image class="cover" :src="recipe.coverUrl || '/static/default-food.png'" mode="aspectFill"/>
    <view class="header-info">
      <text class="name">{{ recipe.name }}</text>
      <text class="meta">{{ recipe.difficulty }} · {{ recipe.cookTime }}分钟 · {{ recipe.servings }}人份</text>
    </view>
    <view class="section"><text class="section-title">食材清单</text></view>
    <view class="ingredient" v-for="i in ingredients" :key="i.id"><text>{{ i.name }}</text><text>{{ i.amount }}</text></view>
    <view class="section"><text class="section-title">烹饪步骤</text></view>
    <view class="step" v-for="s in steps" :key="s.id">
      <text class="step-num">{{ s.stepNum }}</text>
      <view><image v-if="s.imageUrl" :src="s.imageUrl" mode="aspectFill" class="step-img"/><text>{{ s.description }}</text></view>
    </view>
    <view v-if="recipe.tips" class="section">
      <text class="section-title">小贴士</text>
      <text class="tips-text">{{ recipe.tips }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
const recipe = ref<any>({})
const ingredients = ref<any[]>([])
const steps = ref<any[]>([])
onLoad(async (opts:any) => {
  const res = await getApi(`/recipe/${opts.id}`)
  recipe.value = res.recipe
  ingredients.value = res.ingredients || []
  steps.value = res.steps || []
})
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5}
.cover{width:100%;height:400rpx}
.header-info{padding:24rpx;background:#fff}.name{font-size:36rpx;font-weight:700}.meta{font-size:24rpx;color:#999;margin-top:8rpx}
.section{padding:24rpx 24rpx 12rpx}.section-title{font-size:28rpx;font-weight:600;color:#333}
.ingredient{display:flex;justify-content:space-between;padding:16rpx 24rpx;background:#fff;border-bottom:1rpx solid #f0f0f0;font-size:28rpx}
.step{display:flex;gap:16rpx;padding:20rpx 24rpx;background:#fff;margin-bottom:2rpx}
.step-num{width:48rpx;height:48rpx;line-height:48rpx;text-align:center;background:#faad14;color:#fff;border-radius:50%;font-size:24rpx;flex-shrink:0}
.step-img{width:100%;height:200rpx;border-radius:8rpx;margin-bottom:8rpx}
.tips-text{font-size:26rpx;color:#666;padding:16rpx 24rpx;background:#fff;border-radius:12rpx}
</style>