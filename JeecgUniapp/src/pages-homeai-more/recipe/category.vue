<route lang="json5">
{
  style: {
    navigationBarTitleText: '分类浏览',
    navigationBarBackgroundColor: '#F3F2EE',
    onReachBottomDistance: 80,
    enablePullDownRefresh: true,
  },
}
</route>
<template>
  <view class="hai-page">
    <scroll-view scroll-x class="cat-scroll">
      <text
        v-for="c in categories"
        :key="c.id"
        class="cat-tab"
        :class="{ active: activeId === c.id }"
        @click="selectCategory(c.id)"
      >{{ c.name }}</text>
    </scroll-view>
    <view class="grid">
      <view class="card" v-for="r in recipes" :key="r.id" @click="goDetail(r.id)">
        <image class="cover" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" lazy-load />
        <text class="name">{{ r.name }}</text>
        <text class="meta">{{ r.cookTime }}分钟</text>
      </view>
    </view>
    <view v-if="recipes.length > 0" class="load-more-wrap">
      <view v-if="loadingMore" class="load-more-tip">加载中...</view>
      <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
      <view v-else class="load-more-tip">没有更多了</view>
    </view>
    <HomeEmpty
      v-if="!loading && loadFailed"
      title="分类加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      @action="retryLoad"
    />
    <HomeEmpty
      v-else-if="!loading && categories.length === 0"
      title="暂无分类"
      hint="先在管理端或菜谱页维护分类"
    />
    <HomeEmpty
      v-else-if="!loading && recipes.length === 0"
      title="该分类暂无菜谱"
      action-text="添加菜谱"
      @action="goAdd"
    />
  </view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad, onShow, onReachBottom } from '@dcloudio/uni-app'
import { recipeApi } from '../../pages-homeai/api/recipe'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'

useHomeaiPageGuard()
useHomeaiPullRefresh(async () => {
  await loadCategories()
})

const categories = ref<any[]>([])
const recipes = ref<any[]>([])
const activeId = ref('')
const loading = ref(false)
const loadFailed = ref(false)
const PAGE_SIZE = 20
const pageNo = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

async function loadCategories() {
  loadFailed.value = false
  try {
    categories.value = (await recipeApi.categories()) || []
    if (categories.value.length && !activeId.value) {
      activeId.value = categories.value[0].id
    }
    if (activeId.value) await loadRecipes(true)
  } catch {
    loadFailed.value = true
    categories.value = []
  }
}

async function loadRecipes(reset = true, silent = false) {
  if (!activeId.value) return
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
    const res: any = await recipeApi.list({
      categoryId: activeId.value,
      pageNo: String(nextPage),
      pageSize: String(PAGE_SIZE),
    })
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
      } else {
        uni.showToast({ title: '刷新失败', icon: 'none' })
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
  loadRecipes(false)
}

function retryLoad() {
  if (categories.value.length) loadRecipes(true)
  else loadCategories()
}

function selectCategory(id: string) {
  activeId.value = id
  loadRecipes(true)
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${id}` })
}

function goAdd() {
  const q = activeId.value ? `?categoryId=${activeId.value}` : ''
  uni.navigateTo({ url: `/pages-homeai-more/recipe/add${q}` })
}

onLoad(async (opts: any) => {
  if (opts?.categoryId) activeId.value = opts.categoryId
  await loadCategories()
})

onShow(() => {
  if (activeId.value) loadRecipes(true, recipes.value.length > 0)
})

onReachBottom(() => {
  loadMore()
})
</script>
<style scoped>
/* page shell: .hai-page */
.cat-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.cat-tab { display: inline-block; padding: 16rpx 28rpx; background: var(--hai-card); border-radius: 32rpx; margin-right: 12rpx; font-size: 26rpx; color: var(--hai-text-secondary); box-shadow: var(--hai-shadow); }
.cat-tab.active { background: var(--hai-primary); color: var(--hai-on-primary); }
.grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.card { width: calc(50% - 8rpx); background: var(--hai-card); border-radius: 24rpx; overflow: hidden; box-shadow: var(--hai-shadow); }
.cover { width: 100%; height: 200rpx; }
.name { font-size: 26rpx; padding: 12rpx 16rpx 4rpx; display: block; color: var(--hai-text); }
.meta { font-size: 22rpx; color: var(--hai-text-muted); padding: 0 16rpx 16rpx; display: block; }
.load-more-wrap { width: 100%; padding: 24rpx 0 40rpx; text-align: center; }
.load-more-btn { display: inline-block; padding: 16rpx 48rpx; font-size: 26rpx; color: var(--hai-primary); background: var(--hai-card); border-radius: 999rpx; box-shadow: var(--hai-shadow); }
.load-more-tip { font-size: 24rpx; color: var(--hai-text-muted); }
</style>
