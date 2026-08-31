<route lang="json5">
{ style: { navigationBarTitleText: '账单', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true, onReachBottomDistance: 80 } }
</route>

<template>
  <view class="hai-page hai-page--bottom-bar">
    <view class="toolbar">
      <view class="month-nav">
        <text class="month-btn" @click="changeMonth(-1)">‹</text>
        <text class="month-text">{{ currentMonth }}</text>
        <text class="month-btn" @click="changeMonth(1)">›</text>
      </view>
      <view class="toolbar-links">
        <text class="tool-link" @click="goStatistics">统计</text>
        <text class="tool-link" @click="goImport">导入</text>
      </view>
    </view>
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索备注、分类、支付方式" confirm-type="search" @confirm="search" />
      <text v-if="keyword" class="search-clear" @click="clearSearch">清除</text>
    </view>
    <view class="summary">
      <view class="card"><text class="label">{{ monthScope }}支出</text><text class="value red">¥{{ summary.expense }}</text></view>
      <view class="card"><text class="label">{{ monthScope }}收入</text><text class="value green">¥{{ summary.income }}</text></view>
      <view class="card"><text class="label">结余</text><text class="value">¥{{ summary.balance }}</text></view>
    </view>
    <view class="entry" v-for="e in allEntries" :key="e.id" @click="editEntry(e)">
      <view class="entry-left">
        <text class="entry-icon">{{ e.categoryId ? getIcon(e.categoryId) : '💳' }}</text>
        <view>
          <text class="entry-name">{{ e.remark || e.categoryName || e.categoryId }}</text>
          <text class="entry-date">{{ e.billDate }}</text>
        </view>
      </view>
      <text :class="'entry-amount '+(e.type==='income'?'green':'red')">{{ e.type==='income'?'+':'-'}}¥{{ e.amount }}</text>
    </view>
    <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>
    <HomeEmpty
      v-else-if="loadFailed"
      title="账单加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      :card="true"
      @action="keyword.trim() ? search() : loadData()"
    />
    <HomeEmpty
      v-else-if="allEntries.length === 0"
      :title="emptyTitle"
      :hint="keyword.trim() ? '换个词试试，或清空搜索' : '记一笔支出或收入，开始家庭账本'"
      :action-text="keyword.trim() ? '清空搜索' : '记支出'"
      @action="keyword.trim() ? clearSearch() : addEntry('expense')"
    />
    <view v-if="allEntries.length > 0" class="load-more-wrap">
      <text v-if="loadingMore" class="load-more-tip">加载中...</text>
      <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
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
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { billApi } from '../../pages-homeai/api/bill'
import { addMonths, localMonthStr } from '../../pages-homeai/utils/date'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'

useHomeaiPageGuard()
useHomeaiPullRefresh(() => loadData())

const PAGE_SIZE = 20
const allEntries = ref<any[]>([])
const pageNo = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const summary = ref({ expense: '0', income: '0', balance: '0' })
const cats = ref<any[]>([])
const currentMonth = ref(localMonthStr())
const keyword = ref('')
const loading = ref(true)
const loadFailed = ref(false)

const isCurrentMonth = computed(() => currentMonth.value === localMonthStr())
const monthScope = computed(() => (isCurrentMonth.value ? '本月' : currentMonth.value))
const emptyTitle = computed(() => {
  if (keyword.value.trim()) return '未找到相关账单'
  return isCurrentMonth.value ? '本月暂无账单' : `${currentMonth.value} 暂无账单`
})

function applyPage(page: any, append: boolean) {
  const records = Array.isArray(page) ? page : page?.records || []
  const total = Array.isArray(page) ? records.length : Number(page?.total || records.length)
  allEntries.value = append ? allEntries.value.concat(records) : records
  hasMore.value = allEntries.value.length < total
}

async function loadData(silent = false) {
  if (!silent && allEntries.value.length === 0) loading.value = true
  loadFailed.value = false
  pageNo.value = 1
  try {
    const m = currentMonth.value
    applyPage(await billApi.entries(m, 1, PAGE_SIZE, keyword.value), false)
    const sum: any = await billApi.summary(m)
    summary.value = {
      expense: sum.totalExpense ?? '0',
      income: sum.totalIncome ?? '0',
      balance: sum.balance ?? '0',
    }
    cats.value = (await billApi.categories()) || []
  } catch {
    loadFailed.value = allEntries.value.length === 0
    if (silent && allEntries.value.length > 0) {
      uni.showToast({ title: '刷新失败', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

function changeMonth(delta: number) {
  currentMonth.value = addMonths(currentMonth.value, delta)
  allEntries.value = []
  loadData()
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    pageNo.value += 1
    applyPage(await billApi.entries(currentMonth.value, pageNo.value, PAGE_SIZE, keyword.value), true)
  } catch {
    pageNo.value = Math.max(1, pageNo.value - 1)
    uni.showToast({ title: '加载更多失败', icon: 'none' })
  } finally {
    loadingMore.value = false
  }
}

function search() {
  allEntries.value = []
  loadData()
}

function clearSearch() {
  keyword.value = ''
  search()
}

onShow(() => loadData(allEntries.value.length > 0))
onReachBottom(() => loadMore())

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
.toolbar{display:flex;justify-content:space-between;align-items:center;gap:16rpx;margin-bottom:16rpx}
.month-nav{display:flex;align-items:center;gap:16rpx}
.month-btn{font-size:40rpx;color:var(--hai-primary);padding:0 8rpx;line-height:1}
.month-text{font-size:28rpx;font-weight:600;color:var(--hai-text)}
.toolbar-links{display:flex;gap:24rpx}
.tool-link{font-size:26rpx;color:var(--hai-primary)}
.search-bar{position:relative;padding:8rpx 0 16rpx}
.search-input{height:72rpx;padding:0 88rpx 0 28rpx;background:var(--hai-card);border-radius:999rpx;font-size:28rpx;box-shadow:var(--hai-shadow)}
.search-clear{position:absolute;right:28rpx;top:50%;transform:translateY(-50%);font-size:24rpx;color:var(--hai-primary)}
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
