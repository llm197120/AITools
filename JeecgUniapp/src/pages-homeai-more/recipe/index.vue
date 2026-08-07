<route lang="json5">

{ style: { navigationBarTitleText: '烹饪指南' } }

</route>



<template>

  <view class="page">

    <view class="search-bar"><input class="search-input" v-model="keyword" placeholder="搜索菜谱..." confirm-type="search" @confirm="search"/></view>

    <view class="tab-bar">

      <text :class="['tab', tab === 'all' ? 'active' : '']" @click="switchTab('all')">全部</text>

      <text :class="['tab', tab === 'favorite' ? 'active' : '']" @click="switchTab('favorite')">我的收藏</text>

      <text class="tab link" @click="goCategory">分类 ›</text>

    </view>

    <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>

    <view v-else class="recipe-grid">

      <view class="recipe-card" v-for="r in recipes" :key="r.id" @click="detail(r.id)">

        <image class="recipe-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill"/>

        <view class="recipe-info">

          <text class="recipe-name">{{ r.name }}</text>

          <text class="recipe-meta">{{ diffLabel(r.difficulty) }} · {{ r.cookTime }}分钟</text>

        </view>

      </view>

    </view>

    <HomeEmpty v-if="!loading && recipes.length === 0" :title="tab === 'favorite' ? '暂无收藏' : '暂无菜谱'" action-text="添加菜谱" @action="goAdd" />

    <view class="fab" @click="goAdd">+</view>

  </view>

</template>



<script lang="ts" setup>

import { ref } from 'vue'

import { onShow } from '@dcloudio/uni-app'

import { recipeApi } from '../../pages-homeai/api/recipe'

import HomeSkeleton from '../../components/HomeSkeleton.vue'

import HomeEmpty from '../../components/HomeEmpty.vue'



const keyword = ref('')

const recipes = ref<any[]>([])

const tab = ref<'all' | 'favorite'>('all')

const loading = ref(false)



async function load() {

  loading.value = true

  try {

    if (tab.value === 'favorite') {

      recipes.value = (await recipeApi.favorites()) || []

    } else {

      const res = await recipeApi.list()

      recipes.value = res?.records || res || []

    }

  } finally {

    loading.value = false

  }

}



function switchTab(t: 'all' | 'favorite') {

  tab.value = t

  load()

}



onShow(load)



function diffLabel(d: any) {

  const map: Record<number, string> = { 1: '简单', 2: '简单', 3: '中等', 4: '中等', 5: '困难' }

  return map[Number(d)] || '中等'

}



async function search() {

  if (!keyword.value.trim()) { await load(); return }

  loading.value = true

  try {

    const { get } = await import('../../pages-homeai/api/request')

    const res: any = await get('/recipe/search', { keyword: keyword.value.trim() })

    recipes.value = Array.isArray(res) ? res : []

  } finally {

    loading.value = false

  }

}



function detail(id: string) { uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${id}` }) }

function goAdd() { uni.navigateTo({ url: '/pages-homeai-more/recipe/add' }) }

function goCategory() { uni.navigateTo({ url: '/pages-homeai-more/recipe/category' }) }

</script>



<style scoped>

.page{min-height:100vh;background:#f5f5f5;padding:20rpx;padding-bottom:120rpx}

.search-bar{padding:16rpx 0;margin-bottom:12rpx}

.search-input{height:68rpx;padding:0 24rpx;background:#fff;border-radius:34rpx;font-size:28rpx}

.tab-bar{display:flex;background:#fff;border-radius:12rpx;margin-bottom:20rpx;overflow:hidden}

.tab{flex:1;text-align:center;padding:20rpx;font-size:28rpx;color:#666}

.tab.active{color:#fa709a;font-weight:600;border-bottom:4rpx solid #fa709a}

.tab.link{flex:0.8;color:#667eea;font-size:26rpx}

.recipe-grid{display:flex;flex-wrap:wrap;gap:16rpx}

.recipe-card{width:calc(50% - 8rpx);background:#fff;border-radius:12rpx;overflow:hidden}

.recipe-img{width:100%;height:200rpx}

.recipe-info{padding:16rpx}.recipe-name{font-size:26rpx;font-weight:500}.recipe-meta{font-size:22rpx;color:#999;margin-top:4rpx;display:block}

.fab{position:fixed;right:40rpx;bottom:100rpx;width:100rpx;height:100rpx;background:#faad14;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:48rpx;color:#fff}

</style>

