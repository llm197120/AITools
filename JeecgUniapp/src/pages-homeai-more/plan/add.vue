<route lang="json5">{ style: { navigationBarTitleText: '新增计划' } }</route>
<template>
  <view class="plan-add">
    <input class="title-input" v-model="form.title" placeholder="计划标题..." />
    <textarea class="content-input" v-model="form.content" placeholder="详细内容（可选）" />
    <view class="row">
      <text>日期</text>
      <picker mode="date" :value="form.planDate" @change="(e: any) => (form.planDate = e.detail.value)">
        <text>{{ form.planDate }}</text>
      </picker>
    </view>
    <view class="row">
      <text>优先级</text>
      <picker :range="priorityLabels" @change="onPriorityChange">
        <text>{{ priorityLabels[priorityIndex] }}</text>
      </picker>
    </view>
    <view class="row">
      <text>分类</text>
      <picker :range="categoryNames" @change="onCategoryChange">
        <text>{{ form.category || '请选择' }}</text>
      </picker>
    </view>
    <view class="row">
      <text>全天</text>
      <switch :checked="form.isAllDay === 1" @change="(e: any) => (form.isAllDay = e.detail.value ? 1 : 0)" />
    </view>
    <view class="row">
      <text>重复</text>
      <picker :range="repeatLabels" @change="onRepeatChange">
        <text>{{ repeatLabels[repeatIndex] }}</text>
      </picker>
    </view>
    <view class="row">
      <text>提前提醒(分钟)</text>
      <picker :range="['不提醒','15分钟','30分钟','60分钟']" @change="onRemindChange">
        <text>{{ form.remindMinutes === 0 ? '不提醒' : form.remindMinutes + '分钟' }}</text>
      </picker>
    </view>
    <wd-button size="large" type="primary" @click="save">保存</wd-button>
  </view>
</template>
<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { planApi, configApi } from '../../pages-homeai/api/index'

const priorityLabels = ['普通', '重要', '紧急']
const priorityValues = ['normal', 'important', 'urgent']
const priorityIndex = ref(0)
const categoryNames = ref<string[]>([])
const categories = ref<any[]>([])

const repeatLabels = ['不重复', '每天', '每周', '每月']
const repeatValues = ['none', 'daily', 'weekly', 'monthly']
const repeatIndex = ref(0)

const form = ref({
  title: '',
  content: '',
  planDate: new Date().toISOString().substring(0, 10),
  priority: 'normal',
  category: '',
  isAllDay: 1,
  remindMinutes: 0,
  repeatRule: 'none',
})

onLoad((options: any) => {
  if (options?.planDate) form.value.planDate = options.planDate
})

onMounted(async () => {
  categories.value = (await planApi.categories()) || []
  categoryNames.value = categories.value.map((c) => c.name)
  if (categoryNames.value.length) form.value.category = categoryNames.value[0]
})

function onPriorityChange(e: any) {
  priorityIndex.value = Number(e.detail.value)
  form.value.priority = priorityValues[priorityIndex.value]
}

function onCategoryChange(e: any) {
  const idx = Number(e.detail.value)
  form.value.category = categoryNames.value[idx] || ''
}

function onRepeatChange(e: any) {
  repeatIndex.value = Number(e.detail.value)
  form.value.repeatRule = repeatValues[repeatIndex.value]
}

function onRemindChange(e: any) {
  const mins = [0, 15, 30, 60]
  form.value.remindMinutes = mins[Number(e.detail.value)] ?? 0
}

async function requestPlanSubscribe() {
  if (form.value.remindMinutes <= 0) return
  try {
    const cfg: any = await configApi.wechatPublic()
    const tmplId = cfg?.planRemindTemplateId
    if (!tmplId) return
    // #ifdef MP-WEIXIN
    await new Promise<void>((resolve) => {
      uni.requestSubscribeMessage({
        tmplIds: [tmplId],
        complete: () => resolve(),
      })
    })
    // #endif
  } catch {
    // 订阅失败不阻断创建
  }
}

async function save() {
  if (!form.value.title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  await requestPlanSubscribe()
  await planApi.create(form.value)
  uni.showToast({ title: '创建成功', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 800)
}
</script>
<style scoped>
.plan-add { padding: 30rpx; min-height: 100vh; background: #f5f5f5; }
.title-input { width: 100%; font-size: 32rpx; padding: 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 20rpx; box-sizing: border-box; }
.content-input { width: 100%; min-height: 150rpx; font-size: 28rpx; padding: 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 20rpx; box-sizing: border-box; }
.row { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 16rpx; font-size: 28rpx; }
</style>
