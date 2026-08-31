<route lang="json5">{ style: { navigationBarTitleText: '编辑账单', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view v-if="loading" class="hai-page"><HomeSkeleton variant="card" /></view>
  <HomeEmpty
    v-else-if="loadFailed"
    title="账单加载失败"
    hint="请检查网络后重试"
    action-text="重试"
    :card="true"
    @action="loadEntry"
  />
  <HomeFormCard v-else>
    <view class="type-switch">
      <view class="type-btn" :class="{ active: form.type === 'expense' }" @click="switchType('expense')">支出</view>
      <view class="type-btn" :class="{ active: form.type === 'income', income: form.type === 'income' }" @click="switchType('income')">收入</view>
    </view>
    <view class="amount-input">
      <text class="currency">¥</text>
      <input class="amount" v-model="form.amount" type="digit" placeholder="0.00" />
    </view>
    <view class="category-grid">
      <view
        class="cat-item"
        v-for="c in categories"
        :key="c.id"
        :class="{ selected: form.categoryId === c.id }"
        @click="form.categoryId = c.id"
      >
        <text class="cat-icon">{{ c.icon || '💰' }}</text>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>
    <view class="home-form-group">
      <wd-cell-group border>
        <HomeDateCell v-model="form.billDate" label="日期" title="选择日期" />
        <HomePickerCell
          v-model="form.paymentMethod"
          label="支付方式"
          title="选择支付方式"
          :columns="payColumns"
        />
        <wd-cell title="备注" title-width="180rpx" center>
          <input class="home-form-cell-input" v-model="form.remark" placeholder="可选" />
        </wd-cell>
      </wd-cell-group>
    </view>
    <wd-button class="home-form-save" size="large" type="primary" block round :loading="saving" @click="save">保存</wd-button>
    <wd-button size="large" type="error" plain block custom-class="delete-btn" @click="remove">删除此账单</wd-button>
  </HomeFormCard>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { localDateStr } from '../../pages-homeai/utils/date'
import HomeFormCard from '../../components/HomeFormCard.vue'
import HomePickerCell from '../../pages-homeai/components/HomePickerCell.vue'
import HomeDateCell from '../../pages-homeai/components/HomeDateCell.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()

const entryId = ref('')
const saving = ref(false)
const loading = ref(true)
const loadFailed = ref(false)
const form = ref({
  type: 'expense',
  amount: '',
  categoryId: '',
  billDate: '',
  paymentMethod: '微信',
  remark: '',
})
const categories = ref<any[]>([])
const payColumns = [
  { label: '微信', value: '微信' },
  { label: '支付宝', value: '支付宝' },
  { label: '现金', value: '现金' },
  { label: '银行卡', value: '银行卡' },
  { label: '其他', value: '其他' },
]

async function loadCategories(type: string) {
  categories.value = (await billApi.categories(type)) || []
  if (categories.value.length && !categories.value.find((c) => c.id === form.value.categoryId)) {
    form.value.categoryId = categories.value[0].id
  }
}

function switchType(type: string) {
  form.value.type = type
  loadCategories(type)
}

async function loadEntry() {
  if (!entryId.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const entry = await billApi.entryById(entryId.value)
    entryId.value = entry.id
    form.value = {
      type: entry.type || 'expense',
      amount: String(entry.amount ?? ''),
      categoryId: entry.categoryId || '',
      billDate: entry.billDate || localDateStr(),
      paymentMethod: entry.paymentMethod || '微信',
      remark: entry.remark || '',
    }
    await loadCategories(form.value.type)
  } catch {
    loadFailed.value = true
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onLoad(async (opts: any) => {
  if (!opts?.id) {
    loading.value = false
    loadFailed.value = true
    uni.showToast({ title: '参数错误', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  entryId.value = opts.id
  await loadEntry()
})

async function save() {
  if (saving.value) return
  const raw = String(form.value.amount || '').trim()
  if (!/^\d+(\.\d{1,2})?$/.test(raw)) {
    uni.showToast({ title: '请填写有效金额（最多两位小数）', icon: 'none' })
    return
  }
  const amount = parseFloat(raw)
  if (!form.value.categoryId || !Number.isFinite(amount) || amount <= 0) {
    uni.showToast({ title: '请填写有效金额和分类', icon: 'none' })
    return
  }
  if (form.value.billDate > localDateStr()) {
    const ok = await new Promise<boolean>((resolve) => {
      uni.showModal({
        title: '日期未到',
        content: '这是未来的日期，确定仍要记账？',
        success: (r) => resolve(!!r.confirm),
      })
    })
    if (!ok) return
  }
  form.value.remark = String(form.value.remark || '').trim()
  saving.value = true
  try {
    await billApi.update({
      id: entryId.value,
      ...form.value,
      amount,
    })
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch {
    // request 层已 toast
  } finally {
    saving.value = false
  }
}

function remove() {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复',
    success: async (res) => {
      if (res.confirm) {
        await billApi.remove(entryId.value)
        uni.showToast({ title: '已删除', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
      }
    },
  })
}
</script>
<style scoped>
.type-switch { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.type-btn { flex: 1; text-align: center; padding: 22rpx; background: var(--hai-card); border-radius: 999rpx; font-size: 28rpx; color: var(--hai-text-secondary); box-shadow: var(--hai-shadow); }
.type-btn.active { background: var(--hai-primary); color: var(--hai-on-primary); }
.type-btn.active.income { background: var(--hai-success); }
.amount-input { text-align: center; padding: 48rpx 0; background: var(--hai-card); border-radius: var(--hai-radius); margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.currency { font-size: 48rpx; color: var(--hai-text-muted); }
.amount { display: inline-block; width: 300rpx; font-size: 64rpx; text-align: center; border-bottom: 2rpx solid var(--hai-border); margin-left: 12rpx; color: var(--hai-text); }
.category-grid { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 24rpx; }
.cat-item { padding: 20rpx 16rpx; background: var(--hai-card); border-radius: var(--hai-radius-sm); font-size: 26rpx; width: calc(25% - 12rpx); box-sizing: border-box; text-align: center; box-shadow: var(--hai-shadow); border: 1rpx solid transparent; }
.cat-item.selected { background: var(--hai-primary-soft); border-color: var(--hai-primary); }
.cat-name { display: block; margin-top: 6rpx; text-align: center; font-size: 22rpx; color: var(--hai-text-secondary); }
.delete-btn { margin-top: 24rpx; }
</style>
