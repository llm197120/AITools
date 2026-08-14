<route lang="json5">
{ style: { navigationBarTitleText: '烹饪指南', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="hai-page hai-page--fab">
    <view class="search-bar"><input class="search-input" v-model="keyword" placeholder="搜索菜谱..." confirm-type="search" @confirm="search"/></view>
    <scroll-view v-if="recommends.length" scroll-x class="recommend-scroll">
      <view class="recommend-head"><text class="recommend-title">为你推荐</text></view>
      <view class="recommend-row">
        <view class="recommend-card" v-for="r in recommends" :key="r.id" @click="detail(r.id)">
          <image class="recommend-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" lazy-load />
          <text class="recommend-name">{{ r.name }}</text>
          <text class="recommend-reason">{{ reasonLabel(r.reason, r.cookCount) }}</text>
        </view>
      </view>
    </scroll-view>
    <scroll-view v-if="newRecipes.length" scroll-x class="recommend-scroll">
      <view class="recommend-head"><text class="recommend-title">新菜尝鲜</text></view>
      <view class="recommend-row">
        <view class="recommend-card" v-for="r in newRecipes" :key="'n-' + r.id" @click="detail(r.id)">
          <image class="recommend-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" lazy-load />
          <text class="recommend-name">{{ r.name }}</text>
          <text class="recommend-reason">新上架</text>
        </view>
      </view>
    </scroll-view>
    <view class="tab-bar">
      <text :class="['tab', tab === 'all' ? 'active' : '']" @click="switchTab('all')">全部</text>
      <text :class="['tab', tab === 'hot' ? 'active' : '']" @click="switchTab('hot')">热门</text>
      <text :class="['tab', tab === 'favorite' ? 'active' : '']" @click="switchTab('favorite')">收藏</text>
      <text class="tab link" @click="goCategory">分类 ›</text>
    </view>
    <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>
    <view v-else class="recipe-grid">
      <view class="recipe-card" v-for="r in recipes" :key="r.id" @click="detail(r.id)">
        <image class="recipe-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" lazy-load />
        <view class="recipe-info">
          <text class="recipe-name">{{ r.name }}</text>
          <view class="recipe-meta-row">
            <text class="recipe-meta">{{ diffLabel(r.difficulty) }} · {{ r.cookTime }}分钟</text>
            <text v-if="tab === 'hot'" class="tag-views">{{ r.viewCount || 0 }} 次浏览</text>
            <text v-else-if="r.visibility === 'public'" class="tag-public">公开</text>
            <text v-else-if="r.visibility === 'family'" class="tag-family">家庭</text>
          </view>
        </view>
      </view>
    </view>
    <HomeEmpty
      v-if="!loading && recipes.length === 0"
      :title="emptyTitle"
      action-text="添加菜谱"
      @action="goAdd"
    />
    <view class="hai-fab" @click="goAdd">
      <wd-icon name="add" size="24px" color="#fff" />
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { recipeApi } from '../../pages-homeai/api/recipe'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const keyword = ref('')
const recipes = ref<any[]>([])
const recommends = ref<any[]>([])
const newRecipes = ref<any[]>([])
const tab = ref<'all' | 'hot' | 'favorite'>('all')
const loading = ref(false)

const emptyTitle = computed(() => {
  if (tab.value === 'favorite') return '暂无收藏'
  if (tab.value === 'hot') return '暂无热门菜谱'
  return '暂无菜谱'
})

function reasonLabel(reason?: string, cookCount?: number) {
  const map: Record<string, string> = {
    today_plan: '今日计划',
    my_favorite: '我的收藏',
    family_favorite: '家庭常做',
    cooked: '做过多次',
    season: '当季推荐',
    hot: '热门',
  }
  const base = map[reason || ''] || '推荐'
  if (cookCount && cookCount > 0) {
    return reason === 'cooked' ? `做过 ${cookCount} 次` : `${base} · 做过 ${cookCount} 次`
  }
  return base
}

// 推荐/新菜为慢变数据，60s TTL 缓存（主列表 load() 保持实时）
const REC_LOAD_TTL = 60 * 1000
let lastRecLoadAt = 0
let lastNewLoadAt = 0

async function loadRecommend() {
  if (Date.now() - lastRecLoadAt < REC_LOAD_TTL && recommends.value.length) return
  try {
    recommends.value = (await recipeApi.recommend(8)) || []
    lastRecLoadAt = Date.now()
  } catch {
    recommends.value = []
  }
}

async function loadNewRecipes() {
  if (Date.now() - lastNewLoadAt < REC_LOAD_TTL && newRecipes.value.length) return
  try {
    newRecipes.value = (await recipeApi.newest(8, 30)) || []
    lastNewLoadAt = Date.now()
  } catch {
    newRecipes.value = []
  }
}

async function load() {
  loading.value = true
  try {
    if (tab.value === 'favorite') {
      recipes.value = (await recipeApi.favorites()) || []
    } else if (tab.value === 'hot') {
      recipes.value = (await recipeApi.hot(30)) || []
    } else {
      const res = await recipeApi.list()
      recipes.value = res?.records || res || []
    }
  } finally {
    loading.value = false
  }
}

function switchTab(t: 'all' | 'hot' | 'favorite') {
  tab.value = t
  load()
}

onShow(() => {
  loadRecommend()
  loadNewRecipes()
  load()
})

function diffLabel(d: any) {
  const map: Record<number, string> = { 1: '入门', 2: '简单', 3: '中等', 4: '较难', 5: '困难' }
  return map[Number(d)] || '中等'
}

async function search() {
  if (!keyword.value.trim()) {
    await load()
    return
  }
  loading.value = true
  try {
    const { get } = await import('../../pages-homeai/api/request')
    const res: any = await get('/recipe/search', { keyword: keyword.value.trim() })
    recipes.value = Array.isArray(res) ? res : []
    tab.value = 'all'
  } finally {
    loading.value = false
  }
}

function detail(id: string) {
  uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${id}` })
}
function goAdd() {
  uni.navigateTo({ url: '/pages-homeai-more/recipe/add' })
}
function goCategory() {
  uni.navigateTo({ url: '/pages-homeai-more/recipe/category' })
}
</script>

<style scoped>
/* page shell: .hai-page */
.search-bar{padding:8rpx 0 16rpx}
.search-input{height:72rpx;padding:0 28rpx;background:var(--hai-card);border-radius:999rpx;font-size:28rpx;box-shadow:var(--hai-shadow)}
.recommend-scroll{margin-bottom:16rpx;white-space:nowrap}
.recommend-head{margin-bottom:12rpx}
.recommend-title{font-size:28rpx;font-weight:600;color:var(--hai-text)}
.recommend-row{display:inline-flex;gap:16rpx;padding-bottom:4rpx}
.recommend-card{width:200rpx;background:var(--hai-card);border-radius:20rpx;overflow:hidden;box-shadow:var(--hai-shadow)}
.recommend-img{width:200rpx;height:140rpx;background:var(--hai-border);display:block}
.recommend-name{display:block;padding:10rpx 12rpx 0;font-size:24rpx;font-weight:600;color:var(--hai-text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.recommend-reason{display:block;padding:4rpx 12rpx 12rpx;font-size:20rpx;color:var(--hai-text-muted)}
.tab-bar{display:flex;background:var(--hai-card);border-radius:28rpx;margin-bottom:20rpx;overflow:hidden;box-shadow:var(--hai-shadow)}
.tab{flex:1;text-align:center;padding:22rpx 8rpx;font-size:26rpx;color:var(--hai-text-secondary)}
.tab.active{color:var(--hai-primary);font-weight:600;border-bottom:4rpx solid var(--hai-primary)}
.tab.link{flex:0.7;color:var(--hai-primary);font-size:24rpx}
.recipe-grid{display:flex;flex-wrap:wrap;gap:16rpx}
.recipe-card{width:calc(50% - 8rpx);background:var(--hai-card);border-radius:24rpx;overflow:hidden;box-shadow:var(--hai-shadow)}
.recipe-img{width:100%;height:200rpx;background:var(--hai-border)}
.recipe-info{padding:18rpx}
.recipe-name{font-size:26rpx;font-weight:600;color:var(--hai-text)}
.recipe-meta-row{display:flex;align-items:center;flex-wrap:wrap;gap:8rpx;margin-top:6rpx}
.recipe-meta{font-size:22rpx;color:var(--hai-text-muted)}
.tag-public{font-size:20rpx;color:var(--hai-primary)}
.tag-family{font-size:20rpx;color:#2d6a4f}
.tag-views{font-size:20rpx;color:var(--hai-text-muted)}
</style>
