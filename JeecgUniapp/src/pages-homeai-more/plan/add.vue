<route lang="json5">{ style: { navigationBarTitleText: '新增计划', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <HomeFormCard>
    <input class="home-form-input" v-model="form.title" placeholder="计划标题..." />
    <textarea class="home-form-textarea" v-model="form.content" placeholder="详细内容（可选）" />
    <view class="home-form-row">
      <text>日期</text>
      <picker mode="date" :value="form.planDate" @change="(e: any) => (form.planDate = e.detail.value)">
        <text class="home-form-value">{{ form.planDate }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>优先级</text>
      <picker :range="priorityLabels" @change="onPriorityChange">
        <text class="home-form-value">{{ priorityLabels[priorityIndex] }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>分类</text>
      <picker :range="categoryNames" @change="onCategoryChange">
        <text class="home-form-value">{{ form.category || '请选择' }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>全天</text>
      <switch :checked="form.isAllDay === 1" @change="(e: any) => (form.isAllDay = e.detail.value ? 1 : 0)" />
    </view>
    <view class="home-form-row">
      <text>重复</text>
      <picker :range="repeatLabels" @change="onRepeatChange">
        <text class="home-form-value">{{ repeatLabels[repeatIndex] }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>提前提醒(分钟)</text>
      <picker :range="['不提醒','15分钟','30分钟','60分钟']" @change="onRemindChange">
        <text class="home-form-value">{{ form.remindMinutes === 0 ? '不提醒' : form.remindMinutes + '分钟' }}</text>
      </picker>
    </view>
    <view class="home-form-row">
      <text>关联菜谱</text>
      <picker :range="recipeNames" @change="onRecipeChange">
        <text class="home-form-value">{{ selectedRecipeName }}</text>
      </picker>
    </view>
    <wd-button size="large" type="primary" block @click="save">保存</wd-button>
  </HomeFormCard>
</template>
<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { planApi, configApi, recipeApi } from '../../pages-homeai/api/index'
import { localDateStr } from '../../pages-homeai/utils/date'
import HomeFormCard from '../../components/HomeFormCard.vue'

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
  planDate: localDateStr(),
  priority: 'normal',
  category: '',
  isAllDay: 1,
  remindMinutes: 0,
  repeatRule: 'none',
  recipeId: '',
})

const recipes = ref<any[]>([])
const recipeNames = ref<string[]>(['不关联'])
const selectedRecipeName = ref('不关联')

onLoad((options: any) => {
  if (options?.planDate) form.value.planDate = options.planDate
})

onMounted(async () => {
  categories.value = (await planApi.categories()) || []
  categoryNames.value = categories.value.map((c) => c.name)
  if (categoryNames.value.length) form.value.category = categoryNames.value[0]
  try {
    const page: any = await recipeApi.list({ pageNo: '1', pageSize: '50' })
    recipes.value = page?.records || (Array.isArray(page) ? page : [])
  } catch {
    recipes.value = []
  }
  recipeNames.value = ['不关联', ...recipes.value.map((r) => r.name).filter(Boolean)]
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

function onRecipeChange(e: any) {
  const idx = Number(e.detail.value)
  if (!idx) {
    form.value.recipeId = ''
    selectedRecipeName.value = '不关联'
    return
  }
  const recipe = recipes.value[idx - 1]
  form.value.recipeId = recipe?.id || ''
  selectedRecipeName.value = recipe?.name || '不关联'
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
