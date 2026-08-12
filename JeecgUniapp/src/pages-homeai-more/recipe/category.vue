<route lang="json5">{ style: { navigationBarTitleText: '分类浏览', navigationBarBackgroundColor: '#F3F2EE' } }</route>
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
        <image class="cover" :src="r.coverUrl || '/static/default-food.png'" mode="aspectFill" />
        <text class="name">{{ r.name }}</text>
        <text class="meta">{{ r.cookTime }}分钟</text>
      </view>
    </view>
    <HomeEmpty v-if="!loading && recipes.length === 0" title="该分类暂无菜谱" />
  </view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get as getApi } from '../../pages-homeai/api/request'
import HomeEmpty from '../../components/HomeEmpty.vue'

const categories = ref<any[]>([])
const recipes = ref<any[]>([])
const activeId = ref('')
const loading = ref(false)

async function loadCategories() {
  categories.value = (await getApi('/recipe/category/all')) || []
  if (categories.value.length && !activeId.value) {
    activeId.value = categories.value[0].id
    await loadRecipes()
  }
}

async function loadRecipes() {
  loading.value = true
  try {
    const res: any = await getApi('/recipe/list', { categoryId: activeId.value })
    recipes.value = res?.records || res || []
  } finally {
    loading.value = false
  }
}

function selectCategory(id: string) {
  activeId.value = id
  loadRecipes()
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${id}` })
}

onLoad(async (opts: any) => {
  if (opts?.categoryId) activeId.value = opts.categoryId
  await loadCategories()
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
</style>
