<route lang="json5">
{ style: { navigationBarTitleText: '菜谱详情', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="hai-page hai-page--flush">
    <image class="cover" :src="recipe.coverUrl || '/static/default-food.png'" mode="aspectFill"/>
    <view class="header-info hai-card">
      <text class="name">{{ recipe.name }}</text>
      <text class="meta">{{ diffLabel(recipe.difficulty) }} · {{ recipe.cookTime }}分钟 · {{ recipe.servings }}人份 · {{ visibilityLabel(recipe.visibility) }} · {{ recipe.viewCount || 0 }}次浏览</text>
      <view class="header-actions">
        <view class="act" :class="{ danger: isFavorited }" @click="toggleFavorite">
          <wd-icon name="heart" size="16px" :color="isFavorited ? '#C45C4A' : '#1B4F8A'" />
          <text>{{ isFavorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="act" @click="goEdit" v-if="canEdit">
          <wd-icon name="edit" size="16px" color="#1B4F8A" />
          <text>编辑</text>
        </view>
        <view class="act" @click="copyIngredients" v-if="ingredients.length > 0">
          <wd-icon name="file-copy" size="16px" color="#1B4F8A" />
          <text>复制食材</text>
        </view>
      </view>
    </view>
    <view v-if="recipe.videoUrl" class="video-section hai-card">
      <video class="video" :src="recipe.videoUrl" controls></video>
    </view>
    <view class="section-head"><text class="hai-section-title">食材清单</text></view>
    <view class="list-card hai-card">
      <view class="ingredient" v-for="i in ingredients" :key="i.id"><text>{{ i.name }}</text><text class="amt">{{ i.amount }}</text></view>
    </view>
    <view class="section-head"><text class="hai-section-title">烹饪步骤</text></view>
    <view class="list-card hai-card">
      <view class="step" v-for="s in steps" :key="s.id">
        <text class="step-num">{{ s.stepNum }}</text>
        <view class="step-body"><image v-if="s.imageUrl" :src="s.imageUrl" mode="aspectFill" class="step-img"/><text>{{ s.description }}</text></view>
      </view>
    </view>
    <view v-if="recipe.tips" class="tips-card hai-card">
      <text class="hai-section-title">小贴士</text>
      <text class="tips-text">{{ recipe.tips }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get as getApi, post as postApi } from '../../pages-homeai/api/request'
import { useUserStore } from '../../pages-homeai/stores/user'
import { formatQuantityUnit } from '../../pages-homeai/utils/recipeIngredient'
const recipe = ref<any>({})
const ingredients = ref<any[]>([])
const steps = ref<any[]>([])
const userStore = useUserStore()
const canEdit = ref(false)
const isFavorited = ref(false)
const recipeId = ref('')
function diffLabel(d: any) {
  const map: Record<number, string> = { 1: '简单', 2: '简单', 3: '中等', 4: '中等', 5: '困难' }
  return map[Number(d)] || '中等'
}
function visibilityLabel(v?: string) {
  if (v === 'public') return '公开'
  if (v === 'family') return '家庭共享'
  return '仅自己'
}
onLoad(async (opts:any) => {
  recipeId.value = opts.id
  const res = await getApi(`/recipe/${opts.id}`)
  recipe.value = res.recipe
  ingredients.value = (res.ingredients || []).map((x: any) => ({
    ...x,
    amount: formatQuantityUnit(x.quantity, x.unit, x.amount),
  }))
  steps.value = res.steps || []
  isFavorited.value = !!res.isFavorited
  canEdit.value = !!res.recipe && res.recipe.userId === userStore.userInfo?.id
})

async function toggleFavorite() {
  const res: any = await postApi(`/recipe/${recipeId.value}/favorite`)
  isFavorited.value = !!res?.favorited
  uni.showToast({ title: isFavorited.value ? '已收藏' : '已取消收藏', icon: 'none' })
}

function goEdit() {
  uni.navigateTo({ url: `/pages-homeai-more/recipe/add?id=${recipe.value.id}` })
}

function copyIngredients() {
  const text = ingredients.value
    .map((x: any) => `${x.name} ${formatQuantityUnit(x.quantity, x.unit, x.amount)}`.trim())
    .join('\n')
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '食材已复制', icon: 'success' }),
  })
}
</script>

<style scoped>
.hai-page--flush { padding-bottom: 48rpx; padding-top: 0; }
.cover { width: 100%; height: 400rpx; }
.header-info {
  margin: -32rpx 32rpx 20rpx;
  padding: 28rpx 28rpx 24rpx;
  position: relative;
  z-index: 1;
}
.name {
  font-family: var(--hai-serif);
  font-size: 40rpx;
  font-weight: 700;
  color: var(--hai-text);
  display: block;
}
.meta {
  font-size: 24rpx;
  color: var(--hai-text-muted);
  margin-top: 8rpx;
  display: block;
}
.header-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 20rpx;
  flex-wrap: wrap;
}
.act {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 26rpx;
  color: var(--hai-primary);
}
.act.danger {
  color: var(--hai-danger);
}
.video-section {
  margin: 0 32rpx 20rpx;
  padding: 16rpx;
  overflow: hidden;
}
.video {
  width: 100%;
  height: 360rpx;
  border-radius: var(--hai-radius-md);
}
.section-head {
  padding: 8rpx 40rpx 16rpx;
}
.list-card {
  margin: 0 32rpx 24rpx;
  overflow: hidden;
}
.ingredient {
  display: flex;
  justify-content: space-between;
  padding: 22rpx 28rpx;
  border-bottom: 1rpx solid var(--hai-border);
  font-size: 28rpx;
  color: var(--hai-text);
}
.ingredient:last-child {
  border-bottom: none;
}
.amt {
  color: var(--hai-text-secondary);
}
.step {
  display: flex;
  gap: 16rpx;
  padding: 22rpx 28rpx;
  border-bottom: 1rpx solid var(--hai-border);
}
.step:last-child {
  border-bottom: none;
}
.step-num {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  background: var(--hai-primary);
  color: var(--hai-on-primary);
  border-radius: 50%;
  font-size: 24rpx;
  flex-shrink: 0;
}
.step-body {
  flex: 1;
  font-size: 28rpx;
  color: var(--hai-text);
  line-height: 1.5;
}
.step-img {
  width: 100%;
  height: 200rpx;
  border-radius: 12rpx;
  margin-bottom: 8rpx;
}
.tips-card {
  margin: 0 32rpx 32rpx;
  padding: 24rpx 28rpx;
}
.tips-text {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--hai-text-secondary);
  line-height: 1.5;
}
</style>
