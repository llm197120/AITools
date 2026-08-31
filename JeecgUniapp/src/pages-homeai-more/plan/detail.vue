<route lang="json5">{ style: { navigationBarTitleText: '计划详情', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view v-if="loading" class="hai-page"><HomeSkeleton variant="card" /></view>
  <HomeEmpty
    v-else-if="loadFailed"
    title="计划加载失败"
    hint="请检查网络后重试"
    action-text="重试"
    :card="true"
    @action="loadPlan"
  />
  <HomeEmpty
    v-else-if="!plan.id"
    title="计划不存在或已删除"
    hint="可能已被删除，或链接已失效"
    action-text="返回"
    :card="true"
    @action="goBack"
  />
  <view class="hai-page" v-else>
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
      <view class="info-row" v-if="plan.isAllDay !== 1">
        <text class="label">开始时间</text>
        <text>{{ formatHm(plan.startTime) }}</text>
      </view>
      <view class="info-row" v-if="plan.isAllDay !== 1 && Number(plan.remindMinutes) > 0">
        <text class="label">提醒</text>
        <text>提前 {{ plan.remindMinutes }} 分钟</text>
      </view>
      <view class="info-row" v-if="plan.recipeId">
        <text class="label">关联菜谱</text>
        <text class="recipe-link" @click="goRecipe">{{ plan.recipeName || '查看菜谱' }}</text>
      </view>
    </view>
    <view class="content-card" v-if="plan.content">
      <text class="content-label">详细内容</text>
      <text class="content-body">{{ plan.content }}</text>
    </view>
    <view class="actions">
      <wd-button
        v-if="plan.status === 'pending'"
        size="large"
        type="primary"
        block
        :loading="toggling"
        @click="toggleComplete"
      >标记为已完成</wd-button>
      <wd-button
        v-else-if="plan.status === 'completed'"
        size="large"
        plain
        block
        :loading="toggling"
        @click="toggleComplete"
      >取消完成</wd-button>
      <text v-if="plan.status === 'expired'" class="expired-tip">已过期，不可修改状态</text>
      <wd-button v-if="canEdit" size="large" plain block @click="goEdit">编辑</wd-button>
      <wd-button v-if="canEdit" size="large" type="error" plain block @click="deleteVisible = true">删除</wd-button>
    </view>
  </view>
  <wd-popup v-model="deleteVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
    <view class="dialog-title">删除计划</view>
    <view class="dialog-body">
      <text class="dialog-hint">{{ deleteHint }}</text>
    </view>
    <view class="dialog-footer">
      <wd-button block @click="deleteVisible = false">取消</wd-button>
      <wd-button type="error" block :loading="deleting" @click="doDelete">确认删除</wd-button>
    </view>
  </wd-popup>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { planApi } from '../../pages-homeai/api/plan'
import { localDateStr } from '../../pages-homeai/utils/date'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const userStore = useUserStore()

const plan = ref<any>({})
const instanceId = ref('')
const planDate = ref('')
const deleteVisible = ref(false)
const loading = ref(true)
const loadFailed = ref(false)
const toggling = ref(false)
const deleting = ref(false)

function goBack() {
  uni.navigateBack()
}

const canEdit = computed(() => {
  const uid = userStore.userInfo?.id
  return !!uid && !!plan.value.userId && plan.value.userId === uid
})

const deleteHint = computed(() => {
  if (plan.value.repeatRule && plan.value.repeatRule !== 'none') {
    return '删除后该计划将从所有日期消失，确定继续？'
  }
  return '确定删除该计划？'
})

function statusLabel(s: string) {
  return { pending: '待完成', completed: '已完成', expired: '已过期', cancelled: '已取消' }[s] || s
}
function priorityLabel(p: string) {
  return { normal: '普通', important: '重要', urgent: '紧急' }[p] || '普通'
}
function repeatLabel(r: string) {
  return { none: '不重复', daily: '每天', weekly: '每周', monthly: '每月' }[r] || '不重复'
}
function formatHm(v: any) {
  if (!v) return '-'
  if (typeof v === 'string') return v.substring(0, 5)
  if (typeof v === 'object' && v.hour != null) {
    return `${String(v.hour).padStart(2, '0')}:${String(v.minute || 0).padStart(2, '0')}`
  }
  return String(v)
}

async function loadPlan(silent = false) {
  if (!instanceId.value) {
    loading.value = false
    loadFailed.value = true
    return
  }
  if (!silent) loading.value = true
  loadFailed.value = false
  try {
    try {
      const found = await planApi.instance(instanceId.value)
      if (found) {
        plan.value = found
        return
      }
    } catch {
      // 回退按日列表，兼容旧后端
    }
    const list = (await planApi.byDate(planDate.value)) || []
    const found = list.find((p: any) => p.id === instanceId.value)
    if (found) {
      plan.value = found
      return
    }
    if (!plan.value.id) plan.value = {}
  } catch {
    loadFailed.value = !plan.value.id
    if (loadFailed.value) uni.showToast({ title: '计划加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function toggleComplete() {
  if (plan.value.status === 'expired' || toggling.value) return
  toggling.value = true
  try {
    await planApi.toggle(instanceId.value)
    uni.showToast({ title: '已更新', icon: 'success' })
    await loadPlan(true)
  } finally {
    toggling.value = false
  }
}

function goEdit() {
  uni.navigateTo({ url: `/pages-homeai-more/plan/add?id=${instanceId.value}` })
}

async function doDelete() {
  if (deleting.value) return
  deleting.value = true
  try {
    await planApi.remove(instanceId.value)
    deleteVisible.value = false
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    // request 层已 toast
  } finally {
    deleting.value = false
  }
}

function goRecipe() {
  if (!plan.value.recipeId) return
  uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${plan.value.recipeId}` })
}

onLoad((opts: any) => {
  instanceId.value = opts?.id || ''
  planDate.value = opts?.planDate || localDateStr()
  if (!instanceId.value) {
    loading.value = false
    uni.showToast({ title: '参数错误', icon: 'none' })
  }
})

onShow(() => {
  if (instanceId.value) loadPlan(!!plan.value.id)
})
</script>
<style scoped>
/* page shell: .hai-page */
.status-bar { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.priority { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 8rpx; background: var(--hai-border); color: var(--hai-text-secondary); }
.priority.important { background: var(--hai-primary-soft); color: var(--hai-primary); }
.priority.urgent { background: var(--hai-danger-soft); color: var(--hai-danger); }
.status { font-size: 24rpx; color: var(--hai-text-muted); }
.title { font-family: var(--hai-serif); font-size: 40rpx; font-weight: 700; color: var(--hai-text); display: block; margin-bottom: 24rpx; }
.info-card { background: var(--hai-card); border-radius: var(--hai-radius); padding: 24rpx; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.info-row { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid var(--hai-border); font-size: 28rpx; color: var(--hai-text); }
.info-row:last-child { border-bottom: none; }
.label { color: var(--hai-text-secondary); }
.recipe-link { color: var(--hai-primary); }
.content-card {
  background: var(--hai-card);
  border-radius: var(--hai-radius);
  padding: 24rpx;
  margin-bottom: 40rpx;
  box-shadow: var(--hai-shadow);
}
.content-label { display: block; font-size: 24rpx; color: var(--hai-text-secondary); margin-bottom: 12rpx; }
.content-body { font-size: 28rpx; color: var(--hai-text); line-height: 1.6; white-space: pre-wrap; }
.actions { padding: 0 20rpx; display: flex; flex-direction: column; gap: 16rpx; }
.expired-tip { display: block; text-align: center; color: var(--hai-text-muted); font-size: 26rpx; padding: 8rpx; }
.dialog-title {
  font-family: var(--hai-serif);
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text);
}
.dialog-body { padding: 20rpx 30rpx; }
.dialog-hint { display: block; font-size: 26rpx; color: var(--hai-text-secondary); line-height: 1.5; }
.dialog-footer { display: flex; gap: 20rpx; padding: 0 30rpx 30rpx; }
</style>
