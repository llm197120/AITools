<route lang="json5">
{ style: { navigationBarTitleText: '记一笔', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <HomeFormCard>
    <view class="type-switch">
      <view class="type-btn" :class="{ active: form.type === 'expense' }" @click="switchType('expense')">支出</view>
      <view class="type-btn" :class="{ active: form.type === 'income', income: form.type === 'income' }" @click="switchType('income')">收入</view>
    </view>

    <view class="amount-card">
      <view class="amount-input">
        <text class="currency">¥</text>
        <input class="amount" v-model="form.amount" type="digit" placeholder="0.00" />
      </view>
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

    <view class="extra">
      <picker mode="date" :value="form.billDate" @change="onDateChange">
        <view class="extra-item"><text>日期</text><text>{{ form.billDate }}</text></view>
      </picker>
      <picker :range="payMethods" @change="(e: any) => (form.paymentMethod = payMethods[e.detail.value])">
        <view class="extra-item"><text>支付方式</text><text>{{ form.paymentMethod || '选择' }}</text></view>
      </picker>
      <view class="extra-item remark-row">
        <text>备注</text>
        <input class="remark-input" v-model="form.remark" placeholder="可选" />
      </view>
    </view>

    <wd-button size="large" type="primary" block @click="save">保存</wd-button>
  </HomeFormCard>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { localDateStr } from '../../pages-homeai/utils/date'
import HomeFormCard from '../../components/HomeFormCard.vue'

const form = ref({
  type: 'expense',
  amount: '',
  categoryId: '',
  billDate: localDateStr(),
  paymentMethod: '微信',
  remark: '',
})
const categories = ref<any[]>([])
const payMethods = ['微信', '支付宝', '现金', '银行卡', '其他']

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

onLoad(async (opts: any) => {
  if (opts?.type) form.value.type = opts.type
  await loadCategories(form.value.type)
})

function onDateChange(e: any) {
  form.value.billDate = e.detail.value
}

async function save() {
  if (!form.value.amount || !form.value.categoryId) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  await billApi.create({ ...form.value, amount: parseFloat(form.value.amount), source: 'manual' })
  uni.showToast({ title: '保存成功', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 1000)
}
</script>

<style scoped>
.type-switch {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.type-btn {
  flex: 1;
  text-align: center;
  padding: 22rpx;
  background: var(--hai-card);
  border-radius: 999rpx;
  font-size: 28rpx;
  color: var(--hai-text-secondary);
  box-shadow: var(--hai-shadow);
}
.type-btn.active {
  background: var(--hai-primary);
  color: var(--hai-on-primary);
}
.type-btn.active.income {
  background: var(--hai-success);
}
.amount-card {
  background: var(--hai-card);
  border-radius: var(--hai-radius);
  margin-bottom: 24rpx;
  box-shadow: var(--hai-shadow);
}
.amount-input {
  text-align: center;
  padding: 48rpx 0;
}
.currency {
  font-size: 48rpx;
  color: var(--hai-text-muted);
}
.amount {
  display: inline-block;
  width: 300rpx;
  font-size: 64rpx;
  text-align: center;
  border-bottom: 2rpx solid var(--hai-border);
  margin-left: 12rpx;
  color: var(--hai-text);
}
.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.cat-item {
  padding: 20rpx 16rpx;
  background: var(--hai-card);
  border-radius: var(--hai-radius-sm);
  font-size: 26rpx;
  width: calc(25% - 12rpx);
  box-sizing: border-box;
  text-align: center;
  box-shadow: var(--hai-shadow);
  border: 1rpx solid transparent;
}
.cat-item.selected {
  background: var(--hai-primary-soft);
  border-color: var(--hai-primary);
}
.cat-icon {
  display: block;
}
.cat-name {
  display: block;
  margin-top: 6rpx;
  text-align: center;
  font-size: 22rpx;
  color: var(--hai-text-secondary);
}
.extra {
  background: var(--hai-card);
  border-radius: var(--hai-radius);
  margin-bottom: 32rpx;
  overflow: hidden;
  box-shadow: var(--hai-shadow);
}
.extra-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx;
  border-bottom: 1rpx solid var(--hai-border);
  font-size: 28rpx;
  color: var(--hai-text);
}
.extra-item:last-child {
  border-bottom: none;
}
.remark-row {
  gap: 20rpx;
}
.remark-input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: var(--hai-text);
}
</style>
