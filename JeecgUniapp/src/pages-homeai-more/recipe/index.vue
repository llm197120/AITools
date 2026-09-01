<route lang="json5">
{
  style: {
    navigationBarTitleText: '烹饪指南',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="hai-page hai-page--fab">
    <OfflineBanner />
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索菜谱..." confirm-type="search" @confirm="search" />
      <text v-if="keyword" class="search-clear" @click="clearSearch">清除</text>
    </view>
    <scroll-view v-if="recommends.length" scroll-x class="recommend-scroll">
      <view class="recommend-head"><text class="recommend-title">为你推荐</text></view>
      <view class="recommend-row">
        <view class="recommend-card" v-for="r in recommends" :key="r.id" @click="detail(r.id)">
          <OfflineImage class="recommend-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" />
          <text class="recommend-name">{{ r.name }}</text>
          <text class="recommend-reason">{{ reasonLabel(r.reason, r.cookCount) }}</text>
        </view>
      </view>
    </scroll-view>
    <scroll-view v-if="newRecipes.length" scroll-x class="recommend-scroll">
      <view class="recommend-head"><text class="recommend-title">新菜尝鲜</text></view>
      <view class="recommend-row">
        <view class="recommend-card" v-for="r in newRecipes" :key="'n-' + r.id" @click="detail(r.id)">
          <OfflineImage class="recommend-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" />
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
    <HomeEmpty
      v-else-if="loadFailed"
      title="菜谱加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      @action="keyword.trim() ? search() : load(true)"
    />
    <view v-else class="recipe-grid">
      <view class="recipe-card" v-for="r in recipes" :key="r.id" @click="detail(r.id)">
        <OfflineImage class="recipe-img" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" />
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
      v-if="!loading && !loadFailed && recipes.length === 0"
      :title="emptyTitle"
      :action-text="keyword.trim() ? '清空搜索' : '添加菜谱'"
      @action="keyword.trim() ? clearSearch() : goAdd()"
    />
    <view v-if="tab !== 'hot' && recipes.length > 0" class="load-more-wrap">
      <view v-if="loadingMore" class="load-more-tip">加载中...</view>
      <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
      <view v-else class="load-more-tip">没有更多了</view>
    </view>
    <view class="hai-fab" @click="goAdd">
      <wd-icon name="add" size="24px" color="#fff" />
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { recipeApi } from '../../pages-homeai/api/recipe'
import { readList } from '../../pages-homeai/offline/dataAccess'
import OfflineBanner from '../../pages-homeai/offline/OfflineBanner.vue'
import OfflineImage from '../../pages-homeai/offline/OfflineImage.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  lastNewLoadAt = 0
  await Promise.all([loadRecommend(), loadNewRecipes(), keyword.value.trim() ? search() : load(true)])
})

const keyword = ref('')
const recipes = ref<any[]>([])
const recommends = ref<any[]>([])
const newRecipes = ref<any[]>([])
const tab = ref<'all' | 'hot' | 'favorite'>('all')
const loading = ref(false)
const loadFailed = ref(false)
const PAGE_SIZE = 20
const pageNo = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

const emptyTitle = computed(() => {
  if (keyword.value.trim()) return '未找到相关菜谱'
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
    if (!recommends.value.length) recommends.value = []
  }
}

async function loadNewRecipes() {
  if (Date.now() - lastNewLoadAt < REC_LOAD_TTL && newRecipes.value.length) return
  try {
    newRecipes.value = (await recipeApi.newest(8, 30)) || []
    lastNewLoadAt = Date.now()
  } catch {
    if (!newRecipes.value.length) newRecipes.value = []
  }
}

async function load(reset = true, silent = false) {
  if (tab.value === 'hot') {
    if (!silent) loading.value = true
    loadFailed.value = false
    try {
      const r = await readList<any[]>('recipe', 'hot', () => recipeApi.hot(30))
      recipes.value = r.data || []
      if (r.offline) uni.showToast({ title: '离线模式，展示本地数据', icon: 'none' })
    } catch {
      if (!silent) {
        recipes.value = []
        loadFailed.value = true
      }
    } finally {
      loading.value = false
    }
    return
  }
  if (!reset && (loadingMore.value || !hasMore.value)) return
  if (reset) {
    if (!silent) loading.value = true
    loadFailed.value = false
    pageNo.value = 1
    hasMore.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const nextPage = reset ? 1 : pageNo.value + 1
    let res: any
    if (reset) {
      const kw = keyword.value.trim() || 'all'
      const r = await readList<any>(
        'recipe',
        tab.value + ':' + kw,
        () =>
          tab.value === 'favorite'
            ? recipeApi.favorites({ pageNo: '1', pageSize: String(PAGE_SIZE) })
            : recipeApi.list({ pageNo: '1', pageSize: String(PAGE_SIZE) }),
      )
      res = r.data
      if (r.offline) uni.showToast({ title: '离线模式，展示本地数据', icon: 'none' })
    } else {
      const params = { pageNo: String(nextPage), pageSize: String(PAGE_SIZE) }
      res = tab.value === 'favorite' ? await recipeApi.favorites(params) : await recipeApi.list(params)
    }
    const records: any[] = res?.records || (Array.isArray(res) ? res : [])
    recipes.value = reset ? records : recipes.value.concat(records)
    pageNo.value = nextPage
    const total = res?.total
    hasMore.value = typeof total === 'number' ? recipes.value.length < total : records.length >= PAGE_SIZE
  } catch {
    if (reset) {
      if (!silent) {
        recipes.value = []
        loadFailed.value = true
      }
    } else {
      uni.showToast({ title: '加载更多失败', icon: 'none' })
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (tab.value === 'hot') return
  load(false)
}

function switchTab(t: 'all' | 'hot' | 'favorite') {
  keyword.value = ''
  tab.value = t
  load(true)
}

onShow(() => {
  lastNewLoadAt = 0
  loadRecommend()
  loadNewRecipes()
  const silent = recipes.value.length > 0
  if (keyword.value.trim()) search(silent)
  else load(true, silent)
})

onReachBottom(() => {
  loadMore()
})

function diffLabel(d: any) {
  const map: Record<number, string> = { 1: '入门', 2: '简单', 3: '中等', 4: '较难', 5: '困难' }
  return map[Number(d)] || '中等'
}

function clearSearch() {
  keyword.value = ''
  load(true)
}

async function search(silent = false) {
  if (!keyword.value.trim()) {
    await load(true, silent)
    return
  }
  if (!silent) loading.value = true
  loadFailed.value = false
  try {
    const { get } = await import('../../pages-homeai/api/request')
    const res: any = await get('/recipe/search', { keyword: keyword.value.trim() })
    recipes.value = Array.isArray(res) ? res : []
    tab.value = 'all'
    hasMore.value = false
  } catch {
    if (!silent) {
      recipes.value = []
      loadFailed.value = true
    }
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
.search-bar{position:relative;padding:8rpx 0 16rpx}
.search-input{height:72rpx;padding:0 88rpx 0 28rpx;background:var(--hai-card);border-radius:999rpx;font-size:28rpx;box-shadow:var(--hai-shadow)}
.search-clear{position:absolute;right:28rpx;top:50%;transform:translateY(-50%);font-size:24rpx;color:var(--hai-primary)}
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
.load-more-wrap{width:100%;padding:24rpx 0 40rpx;text-align:center}
.load-more-btn{display:inline-block;padding:16rpx 48rpx;font-size:26rpx;color:var(--hai-primary);background:var(--hai-card);border-radius:999rpx;box-shadow:var(--hai-shadow)}
.load-more-tip{font-size:24rpx;color:var(--hai-text-muted)}
</style>
