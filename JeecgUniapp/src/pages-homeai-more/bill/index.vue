<route lang="json5">
{ style: { navigationBarTitleText: '账单' } }
</route>

<template>
  <view class="page">
    <view class="summary">
      <view class="card"><text class="label">本月支出</text><text class="value red">¥{{ summary.expense }}</text></view>
      <view class="card"><text class="label">本月收入</text><text class="value green">¥{{ summary.income }}</text></view>
      <view class="card"><text class="label">结余</text><text class="value">¥{{ summary.balance }}</text></view>
    </view>
    <view class="entry" v-for="e in entries" :key="e.id" @click="editEntry(e)">
      <view class="entry-left">
        <text class="entry-icon">{{ e.categoryId ? getIcon(e.categoryId) : '💳' }}</text>
        <view>
          <text class="entry-name">{{ e.remark || e.categoryName || e.categoryId }}</text>
          <text class="entry-date">{{ e.billDate }}</text>
        </view>
      </view>
      <text :class="'entry-amount '+(e.type==='income'?'green':'red')">{{ e.type==='income'?'+':'-'}}¥{{ e.amount }}</text>
    </view>
    <view class="tabs">
      <view class="tab" @click="addEntry('expense')">💸 记支出</view>
      <view class="tab" @click="addEntry('income')">💰 记收入</view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, put as putApi, del as delApi } from '../../pages-homeai/api/request'

const entries = ref<any[]>([])
const summary = ref({expense:'0',income:'0',balance:'0'})
const cats = ref<any[]>([])

onShow(async () => {
  const now = new Date(); const m = now.getFullYear()+'-'+String(now.getMonth()+1).padStart(2,'0')
  entries.value = await getApi('/bill/entries', { params: { yearMonth: m } })
  const sum: any = await getApi('/bill/summary', { params: { yearMonth: m } })
  summary.value = { expense: sum.totalExpense ?? '0', income: sum.totalIncome ?? '0', balance: sum.balance ?? '0' }
  cats.value = await getApi('/bill/categories')
})

function getIcon(id:string) { return '📌' }

function addEntry(type:string) {
  uni.showActionSheet({
    itemList: cats.value.map((c:any)=>c.name),
    success: async (r) => {
      const c = cats.value[r.tapIndex]
      uni.showModal({
        title: type==='expense'?'记支出':'记收入',
        editable: true, placeholderText: '金额',
        success: async (res)=>{
          if(res.confirm && res.content){
            await postApi('/bill/entry', { data: { type, categoryId: c.id, amount: parseFloat(res.content), billDate: new Date().toISOString().substring(0,10), paymentMethod:'微信', source:'manual' } })
            onShow()
          }
        }
      })
    }
  })
}

function editEntry(e:any) { uni.showToast({title:'编辑:'+e.remark, icon:'none'}) }
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f5;padding:20rpx}
.summary{display:flex;gap:16rpx;margin-bottom:30rpx}
.card{flex:1;background:#fff;border-radius:12rpx;padding:20rpx;text-align:center}
.label{font-size:22rpx;color:#999}
.value{font-size:36rpx;font-weight:700;display:block;margin-top:8rpx}
.red{color:#e74c3c}.green{color:#27ae60}
.entry{display:flex;align-items:center;justify-content:space-between;padding:24rpx;background:#fff;border-radius:12rpx;margin-bottom:12rpx}
.entry-left{display:flex;align-items:center;gap:16rpx}
.entry-icon{font-size:32rpx}.entry-name{font-size:28rpx}.entry-date{font-size:22rpx;color:#999;display:block}
.entry-amount{font-size:32rpx;font-weight:600}
.tabs{display:flex;gap:16rpx;margin-top:40rpx}
.tab{flex:1;text-align:center;padding:24rpx;background:#667eea;border-radius:12rpx;color:#fff;font-size:28rpx;font-weight:500}
</style>
