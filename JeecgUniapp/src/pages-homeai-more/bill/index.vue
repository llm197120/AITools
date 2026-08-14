<route lang="json5">
{ style: { navigationBarTitleText: '账单', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="hai-page">
    <view class="toolbar">
      <text class="tool-link" @click="goStatistics">统计</text>
      <text class="tool-link" @click="goImport">导入</text>
    </view>
    <view class="summary">
      <view class="card"><text class="label">本月支出</text><text class="value red">¥{{ summary.expense }}</text></view>
      <view class="card"><text class="label">本月收入</text><text class="value green">¥{{ summary.income }}</text></view>
      <view class="card"><text class="label">结余</text><text class="value">¥{{ summary.balance }}</text></view>
    </view>
    <view class="entry" v-for="e in displayedEntries" :key="e.id" @click="editEntry(e)">
      <view class="entry-left">
        <text class="entry-icon">{{ e.categoryId ? getIcon(e.categoryId) : '💳' }}</text>
        <view>
          <text class="entry-name">{{ e.remark || e.categoryName || e.categoryId }}</text>
          <text class="entry-date">{{ e.billDate }}</text>
        </view>
      </view>
      <text :class="'entry-amount '+(e.type==='income'?'green':'red')">{{ e.type==='income'?'+':'-'}}¥{{ e.amount }}</text>
    </view>
    <HomeEmpty
      v-if="allEntries.length === 0"
      title="本月暂无账单"
      hint="记一笔支出或收入，开始家庭账本"
      action-text="记支出"
      @action="addEntry('expense')"
    />
    <view v-if="allEntries.length > 0" class="load-more-wrap">
      <view v-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
      <view v-else class="load-more-tip">没有更多了</view>
    </view>
    <view class="hai-bottom-bar">
      <view class="hai-bottom-btn" @click="addEntry('expense')">记支出</view>
      <view class="hai-bottom-btn success" @click="addEntry('income')">记收入</view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const PAGE_STEP = 20
const allEntries = ref<any[]>([])
const displayCount = ref(PAGE_STEP)
const summary = ref({ expense: '0', income: '0', balance: '0' })
const cats = ref<any[]>([])

const displayedEntries = computed(() => allEntries.value.slice(0, displayCount.value))
const hasMore = computed(() => displayCount.value < allEntries.value.length)

async function loadData() {
  const now = new Date()
  const m = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  allEntries.value = (await billApi.entries(m)) || []
  displayCount.value = PAGE_STEP
  const sum: any = await billApi.summary(m)
  summary.value = {
    expense: sum.totalExpense ?? '0',
    income: sum.totalIncome ?? '0',
    balance: sum.balance ?? '0',
  }
  cats.value = (await billApi.categories()) || []
}

function loadMore() {
  if (!hasMore.value) return
  displayCount.value = Math.min(displayCount.value + PAGE_STEP, allEntries.value.length)
}

onShow(loadData)

function getIcon(id: string) {
  const c = cats.value.find((x: any) => x.id === id)
  return c?.icon || '📌'
}

function addEntry(type: string) {
  uni.navigateTo({ url: `/pages-homeai-more/bill/add?type=${type}` })
}

  function editEntry(e: any) {
    uni.navigateTo({ url: `/pages-homeai-more/bill/edit?id=${e.id}` })
  }

function goStatistics() {
  uni.navigateTo({ url: '/pages-homeai-more/bill/statistics' })
}

function goImport() {
  uni.navigateTo({ url: '/pages-homeai-more/bill/import' })
}
</script>

<style scoped>
/* page shell: .hai-page */
.toolbar{display:flex;justify-content:flex-end;gap:24rpx;margin-bottom:16rpx}
.tool-link{font-size:26rpx;color:var(--hai-primary)}
.summary{display:flex;gap:16rpx;margin-bottom:24rpx}
.card{flex:1;background:var(--hai-card);border-radius:24rpx;padding:24rpx 16rpx;text-align:center;box-shadow:var(--hai-shadow)}
.label{font-size:22rpx;color:var(--hai-text-muted)}
.value{font-size:34rpx;font-weight:700;display:block;margin-top:8rpx;color:var(--hai-text)}
.red{color:var(--hai-danger)}.green{color:var(--hai-success)}
.entry{display:flex;align-items:center;justify-content:space-between;padding:28rpx;background:var(--hai-card);border-radius:24rpx;margin-bottom:16rpx;box-shadow:var(--hai-shadow)}
.entry-left{display:flex;align-items:center;gap:16rpx}
.entry-icon{font-size:32rpx}.entry-name{font-size:28rpx;color:var(--hai-text)}.entry-date{font-size:22rpx;color:var(--hai-text-muted);display:block}
.entry-amount{font-size:32rpx;font-weight:600}
.load-more-wrap{padding:16rpx 0 8rpx;text-align:center}
.load-more-btn{display:inline-block;padding:16rpx 48rpx;font-size:26rpx;color:var(--hai-primary);background:var(--hai-card);border-radius:999rpx;box-shadow:var(--hai-shadow)}
.load-more-tip{font-size:24rpx;color:var(--hai-text-muted)}
</style>
