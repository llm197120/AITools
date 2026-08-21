<route lang="json5">{ style: { navigationBarTitleText: '新增计划', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <HomeFormCard>
    <input class="home-form-input" v-model="form.title" placeholder="计划标题..." />
    <textarea class="home-form-textarea" v-model="form.content" placeholder="详细内容（可选）" />

    <view class="home-form-group">
      <wd-cell-group border>
        <HomeDateCell v-if="!editing" v-model="form.planDate" label="日期" title="选择日期" />
        <HomePickerCell
          v-model="form.priority"
          label="优先级"
          title="选择优先级"
          :columns="priorityColumns"
        />
        <HomePickerCell
          v-model="form.category"
          label="分类"
          title="选择分类"
          placeholder="请选择分类"
          :columns="categoryColumns"
        />
        <wd-cell title="全天" title-width="180rpx" center>
          <wd-switch v-model="allDay" active-color="#1B4F8A" size="22px" />
        </wd-cell>
        <HomeTimeCell v-if="!allDay" v-model="form.startTime" label="开始时间" title="选择开始时间" />
        <HomePickerCell
          v-if="!editing"
          v-model="form.repeatRule"
          label="重复"
          title="重复规则"
          :columns="repeatColumns"
        />
        <HomePickerCell
          v-if="!allDay"
          v-model="remindValue"
          label="提前提醒"
          title="提前提醒"
          :columns="remindColumns"
        />
        <HomePickerCell
          v-model="recipeValue"
          label="关联菜谱"
          title="关联菜谱"
          :columns="recipeColumns"
          :filterable="recipeColumns.length > 8"
        />
      </wd-cell-group>
    </view>
    <text v-if="editing" class="edit-hint">编辑会更新整条计划的标题、内容和提醒；日期与重复规则保持不变。</text>

    <wd-button class="home-form-save" size="large" type="primary" block round @click="save">
      {{ editing ? '保存修改' : '保存' }}
    </wd-button>
  </HomeFormCard>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { planApi, configApi, recipeApi } from '../../pages-homeai/api/index'
import { localDateStr } from '../../pages-homeai/utils/date'
import HomeFormCard from '../../components/HomeFormCard.vue'
import HomePickerCell from '../../pages-homeai/components/HomePickerCell.vue'
import HomeDateCell from '../../pages-homeai/components/HomeDateCell.vue'
import HomeTimeCell from '../../pages-homeai/components/HomeTimeCell.vue'
import { scheduleTodayPlanReminds } from '../../pages-homeai/utils/push'

const priorityColumns = [
  { label: '普通', value: 'normal' },
  { label: '重要', value: 'important' },
  { label: '紧急', value: 'urgent' },
]
const repeatColumns = [
  { label: '不重复', value: 'none' },
  { label: '每天', value: 'daily' },
  { label: '每周', value: 'weekly' },
  { label: '每月', value: 'monthly' },
]
const remindColumns = [
  { label: '不提醒', value: '0' },
  { label: '15分钟', value: '15' },
  { label: '30分钟', value: '30' },
  { label: '60分钟', value: '60' },
]

const categoryColumns = ref<{ label: string; value: string }[]>([])
const recipes = ref<any[]>([])
const editing = ref(false)
const instanceId = ref('')

const form = ref({
  title: '',
  content: '',
  planDate: localDateStr(),
  priority: 'normal',
  category: '',
  isAllDay: 1,
  startTime: '09:00',
  remindMinutes: 0,
  repeatRule: 'none',
  recipeId: '',
})

const allDay = computed({
  get: () => form.value.isAllDay === 1,
  set: (v: boolean) => {
    form.value.isAllDay = v ? 1 : 0
    if (v) {
      form.value.remindMinutes = 0
    } else if (!form.value.startTime) {
      form.value.startTime = '09:00'
    }
  },
})

const remindValue = computed({
  get: () => String(form.value.remindMinutes ?? 0),
  set: (v: string) => {
    form.value.remindMinutes = Number(v) || 0
  },
})

const recipeValue = computed({
  get: () => form.value.recipeId || 'none',
  set: (v: string) => {
    form.value.recipeId = !v || v === 'none' ? '' : v
  },
})

const recipeColumns = computed(() => [
  { label: '不关联', value: 'none' },
  ...recipes.value
    .filter((r) => r?.id && r?.name)
    .map((r) => ({ label: String(r.name), value: String(r.id) })),
])

function toHm(v: any): string {
  if (!v) return '09:00'
  if (typeof v === 'string') return v.substring(0, 5)
  if (typeof v === 'object' && v.hour != null) {
    return `${String(v.hour).padStart(2, '0')}:${String(v.minute || 0).padStart(2, '0')}`
  }
  return '09:00'
}

onLoad(async (options: any) => {
  if (options?.planDate) form.value.planDate = options.planDate
  if (options?.id) {
    editing.value = true
    instanceId.value = options.id
    uni.setNavigationBarTitle({ title: '编辑计划' })
  }
  const list = (await planApi.categories()) || []
  categoryColumns.value = list.map((c: any) => ({ label: c.name, value: c.name }))
  try {
    const page: any = await recipeApi.list({ pageNo: '1', pageSize: '50' })
    recipes.value = page?.records || (Array.isArray(page) ? page : [])
  } catch {
    recipes.value = []
  }
  if (editing.value) {
    await loadExisting()
    return
  }
  if (categoryColumns.value.length && !form.value.category) {
    form.value.category = categoryColumns.value[0].value
  }
})

async function loadExisting() {
  try {
    const inst: any = await planApi.instance(instanceId.value)
    if (!inst) {
      uni.showToast({ title: '计划不存在', icon: 'none' })
      return
    }
    form.value.title = inst.title || ''
    form.value.content = inst.content || ''
    form.value.planDate = inst.planDate || form.value.planDate
    form.value.priority = inst.priority || 'normal'
    form.value.category = inst.category || (categoryColumns.value[0]?.value || '')
    form.value.isAllDay = inst.isAllDay === 0 ? 0 : 1
    form.value.startTime = toHm(inst.startTime)
    form.value.remindMinutes = Number(inst.remindMinutes) || 0
    form.value.repeatRule = inst.repeatRule || 'none'
    form.value.recipeId = inst.recipeId || ''
  } catch {
    // request 层已 toast
  }
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
    // #ifdef APP-PLUS
    // 本地通知在保存成功后由 scheduleTodayPlanReminds 调度
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
  if (!allDay.value && form.value.remindMinutes > 0 && !form.value.startTime) {
    uni.showToast({ title: '设置提醒请先选择开始时间', icon: 'none' })
    return
  }
  const payload = {
    ...form.value,
    startTime: allDay.value ? null : form.value.startTime,
    remindMinutes: allDay.value ? 0 : form.value.remindMinutes,
  }
  try {
    await requestPlanSubscribe()
    if (editing.value) {
      await planApi.update(instanceId.value, payload)
    } else {
      await planApi.create(payload)
    }
    if (form.value.planDate === localDateStr()) {
      await scheduleTodayPlanReminds()
    }
    uni.showToast({ title: editing.value ? '已保存' : '创建成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch {
    // request 层已 toast
  }
}
</script>
<style scoped>
.edit-hint {
  display: block;
  margin: 8rpx 0 16rpx;
  font-size: 22rpx;
  color: var(--hai-text-muted);
  line-height: 1.5;
}
</style>
