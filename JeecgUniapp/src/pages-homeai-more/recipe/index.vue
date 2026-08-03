<route lang="json5">
{ style: { navigationBarTitleText: '烹饪指南' } }
</route>

<template>
  <view class="page">
    <view class="search-bar"><input class="search-input" v-model="keyword" placeholder="搜索菜谱..." confirm-type="search" @confirm="search"/></view>
    <view class="section"><text class="section-title">热门推荐</text></view>
    <view class="recipe-grid">
      <view class="recipe-card" v-for="r in recipes" :key="r.id" @click="detail(r.id)">
        <image class="recipe-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill"/>
        <view class="recipe-info">
          <text class="recipe-name">{{ r.name }}</text>
          <text class="recipe-meta">{{ r.difficulty }} · {{ r.cookTime }}分钟</text>
        </view>
      </view>
    </view>
    <view class="fab" @click="goAdd">+</view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onShow } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
const keyword = ref('')
const recipes = ref<any[]>([])
onShow(async () => { const res = await getApi('/recipe/list'); recipes.value = res.records || res || [] })
function search() { uni.showToast({title:'搜索:'+keyword.value, icon:'none'}) }
function detail(id:string) { uni.navigateTo({url:`/pages/homeai-more/recipe/detail?id=${id}`}) }
function goAdd() { uni.navigateTo({url:'/pages/homeai-more/recipe/add'}) }
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.search-bar{padding:16rpx 0;margin-bottom:20rpx}
.search-input{height:68rpx;padding:0 24rpx;background:#fff;border-radius:34rpx;font-size:28rpx}
.section{padding:20rpx 0}.section-title{font-size:30rpx;font-weight:600}
.recipe-grid{display:flex;flex-wrap:wrap;gap:16rpx}
.recipe-card{width:calc(50% - 8rpx);background:#fff;border-radius:12rpx;overflow:hidden}
.recipe-img{width:100%;height:200rpx}
.recipe-info{padding:16rpx}.recipe-name{font-size:26rpx;font-weight:500}.recipe-meta{font-size:22rpx;color:#999;margin-top:4rpx;display:block}
.fab{position:fixed;right:40rpx;bottom:100rpx;width:100rpx;height:100rpx;background:#faad14;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:48rpx;color:#fff}
</style>