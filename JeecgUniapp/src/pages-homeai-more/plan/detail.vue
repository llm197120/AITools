<route lang="json5">{ style: { navigationBarTitleText: '计划详情', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page" v-if="plan.id">
    <view class="status-bar">
      <text class="priority" :class="plan.priority || 'normal'">{{ priorityLabel(plan.priority) }}</text>
      <text class="status">{{ statusLabel(plan.status) }}</text>
    </view>
    <text class="title">{{ plan.title }}</text>
    <view class="info-card">
      <view class="info-row"><text class="label">日期</text><text>{{ plan.planDate }}</text></view>
      <view class="info-row"><text class="label">分类</text><text>{{ plan.category || '-' }}</text></view>
      <view class="info-row"><text class="label">重复</text><text>{{ repeatLabel(plan.repeatRule) }}</text></view>
      <view class="info-row"><text class="label">全天</text><text>{{ plan.isAllDay === 1 ? '是' : '否' }}</text></view>
      <view class="info-row" v-if="plan.recipeId">
        <text class="label">关联菜谱</text>
        <text class="recipe-link" @click="goRecipe">{{ plan.recipeName || '查看菜谱' }}</text>
      </view>
    </view>
    <view class="actions">
      <wd-button
        v-if="plan.status === 'pending'"
        size="large"
        type="primary"
        @click="toggleComplete"
      >标记为已完成</wd-button>
      <wd-button
        v-else-if="plan.status === 'completed'"
        size="large"
        plain
        @click="toggleComplete"
      >取消完成</wd-button>
      <text v-if="plan.status === 'expired'" class="expired-tip">已过期，不可修改状态</text>
    </view>
  </view>
  <view v-else class="loading">加载中...</view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { planApi } from '../../pages-homeai/api/plan'
import { localDateStr } from '../../pages-homeai/utils/date'

const plan = ref<any>({})
const instanceId = ref('')
const planDate = ref('')

function statusLabel(s: string) {
  return { pending: '待完成', completed: '已完成', expired: '已过期', cancelled: '已取消' }[s] || s
}
function priorityLabel(p: string) {
  return { normal: '普通', important: '重要', urgent: '紧急' }[p] || '普通'
}
function repeatLabel(r: string) {
  return { none: '不重复', daily: '每天', weekly: '每周', monthly: '每月' }[r] || '不重复'
}

async function loadPlan() {
  const list = (await planApi.byDate(planDate.value)) || []
  const found = list.find((p: any) => p.id === instanceId.value)
  if (found) plan.value = found
  else uni.showToast({ title: '计划不存在', icon: 'none' })
}

async function toggleComplete() {
  if (plan.value.status === 'expired') return
  await planApi.toggle(instanceId.value)
  uni.showToast({ title: '已更新', icon: 'success' })
  await loadPlan()
}

function goRecipe() {
  if (!plan.value.recipeId) return
  uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${plan.value.recipeId}` })
}

onLoad(async (opts: any) => {
  instanceId.value = opts?.id || ''
  planDate.value = opts?.planDate || localDateStr()
  if (!instanceId.value) {
    uni.showToast({ title: '参数错误', icon: 'none' })
    return
  }
  await loadPlan()
})
</script>
<style scoped>
/* page shell: .hai-page */
.loading { text-align: center; padding: 80rpx; color: var(--hai-text-muted); }
.status-bar { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.priority { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 8rpx; background: var(--hai-border); color: var(--hai-text-secondary); }
.priority.important { background: var(--hai-primary-soft); color: var(--hai-primary); }
.priority.urgent { background: var(--hai-danger-soft); color: var(--hai-danger); }
.status { font-size: 24rpx; color: var(--hai-text-muted); }
.title { font-family: var(--hai-serif); font-size: 40rpx; font-weight: 700; color: var(--hai-text); display: block; margin-bottom: 24rpx; }
.info-card { background: var(--hai-card); border-radius: var(--hai-radius); padding: 24rpx; margin-bottom: 40rpx; box-shadow: var(--hai-shadow); }
.info-row { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid var(--hai-border); font-size: 28rpx; color: var(--hai-text); }
.info-row:last-child { border-bottom: none; }
.label { color: var(--hai-text-secondary); }
.recipe-link { color: var(--hai-primary); }
.actions { padding: 0 20rpx; }
.expired-tip { display: block; text-align: center; color: var(--hai-text-muted); font-size: 26rpx; padding: 24rpx; }
</style>
