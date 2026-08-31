<route lang="json5">
{ style: { navigationBarTitleText: '回收站', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true, onReachBottomDistance: 80 } }
</route>

<template>
  <view class="hai-page">
    <view class="tab-bar">
      <text :class="['tab', type === 'file' ? 'active' : '']" @click="switchType('file')">文件</text>
      <text :class="['tab', type === 'folder' ? 'active' : '']" @click="switchType('folder')">文件夹</text>
    </view>

    <view v-if="loading"><HomeSkeleton variant="list" :rows="4" /></view>
    <view v-else class="list">
      <view class="item" v-for="row in rows" :key="row.id">
        <view class="item-main" @click="toggleSelect(row.id)">
          <view :class="['check', selected.includes(row.id) ? 'on' : '']" />
          <view class="meta">
            <text class="name">{{ type === 'folder' ? row.name : (row.originalName || row.name) }}</text>
            <text class="sub">{{ row.deletedAt || '已删除' }}</text>
          </view>
        </view>
        <view class="ops">
          <text class="op" @click.stop="restoreOne(row.id)">恢复</text>
          <text class="op danger" @click.stop="purgeOne(row.id)">彻底删除</text>
        </view>
      </view>
      <HomeEmpty
        v-if="!loading && loadFailed"
        title="回收站加载失败"
        hint="请检查网络后重试"
        action-text="重试"
        @action="load"
      />
      <HomeEmpty v-else-if="!loading && rows.length === 0" title="回收站是空的" hint="删除的文件会出现在这里" />
      <view v-if="rows.length > 0" class="load-more-wrap">
        <view v-if="loadingMore" class="load-more-tip">加载中...</view>
        <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
        <view v-else class="load-more-tip">没有更多了</view>
      </view>
    </view>

    <view v-if="selected.length" class="batch-bar">
      <text class="batch-info">已选 {{ selected.length }} 项</text>
      <view class="batch-ops">
        <text class="op" @click="restoreSelected">恢复</text>
        <text class="op danger" @click="purgeSelected">彻底删除</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { storageApi } from '../../pages-homeai/api/storage'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()
useHomeaiPullRefresh(() => load(true))

const type = ref<'file' | 'folder'>('file')
const rows = ref<any[]>([])
const selected = ref<string[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const busy = ref(false)
const PAGE_SIZE = 20
const pageNo = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)

onShow(() => {
  load(true, rows.value.length > 0)
})
onReachBottom(() => loadMore())

function switchType(t: 'file' | 'folder') {
  type.value = t
  selected.value = []
  load(true)
}

function loadMore() {
  load(false)
}

async function load(reset = true, silent = false) {
  if (!reset && (loadingMore.value || !hasMore.value)) return
  if (reset) {
    if (!silent) loading.value = true
    loadFailed.value = false
    pageNo.value = 1
    hasMore.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const nextPage = reset ? 1 : pageNo.value + 1
    const res: any = await storageApi.myRecycleBin({
      type: type.value,
      pageNo: nextPage,
      pageSize: PAGE_SIZE,
    })
    const records = res?.records || []
    rows.value = reset ? records : rows.value.concat(records)
    pageNo.value = nextPage
    const total = res?.total
    hasMore.value = typeof total === 'number' ? rows.value.length < total : records.length >= PAGE_SIZE
    if (reset) selected.value = []
  } catch {
    if (reset) {
      loadFailed.value = rows.value.length === 0
      if (!silent) {
        rows.value = []
      } else if (rows.value.length > 0) {
        uni.showToast({ title: '刷新失败', icon: 'none' })
      }
    } else {
      uni.showToast({ title: '加载更多失败', icon: 'none' })
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function toggleSelect(id: string) {
  if (selected.value.includes(id)) {
    selected.value = selected.value.filter((x) => x !== id)
  } else {
    selected.value = [...selected.value, id]
  }
}

function payload(ids: string[]) {
  return type.value === 'folder' ? { folderIds: ids } : { fileIds: ids }
}

async function restoreOne(id: string) {
  if (busy.value) return
  busy.value = true
  try {
    await storageApi.myRestore(payload([id]))
    uni.showToast({ title: '已恢复', icon: 'success' })
    await load(true)
  } finally {
    busy.value = false
  }
}

async function purgeOne(id: string) {
  uni.showModal({
    title: '彻底删除',
    content: '删除后不可恢复，确定？',
    success: async (r) => {
      if (!r.confirm || busy.value) return
      busy.value = true
      try {
        await storageApi.myDeletePermanently(payload([id]))
        uni.showToast({ title: '已删除', icon: 'success' })
        await load(true)
      } finally {
        busy.value = false
      }
    },
  })
}

async function restoreSelected() {
  if (!selected.value.length || busy.value) return
  busy.value = true
  try {
    await storageApi.myRestore(payload(selected.value))
    uni.showToast({ title: '已恢复', icon: 'success' })
    await load(true)
  } finally {
    busy.value = false
  }
}

async function purgeSelected() {
  if (!selected.value.length) return
  uni.showModal({
    title: '彻底删除',
    content: `确定彻底删除选中的 ${selected.value.length} 项？`,
    success: async (r) => {
      if (!r.confirm || busy.value) return
      busy.value = true
      try {
        await storageApi.myDeletePermanently(payload(selected.value))
        uni.showToast({ title: '已删除', icon: 'success' })
        await load(true)
      } finally {
        busy.value = false
      }
    },
  })
}
</script>

<style scoped lang="scss">
.tab-bar {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx 8rpx;
}
.tab {
  font-size: 28rpx;
  color: var(--hai-text-muted);
  padding-bottom: 8rpx;
}
.tab.active {
  color: var(--hai-primary, #1b4f8a);
  font-weight: 600;
  border-bottom: 4rpx solid var(--hai-primary, #1b4f8a);
}
.list {
  padding: 8rpx 32rpx 160rpx;
}
.item {
  background: var(--hai-card, #fff);
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: var(--hai-shadow);
}
.item-main {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.check {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid #c4bfb6;
}
.check.on {
  background: var(--hai-primary, #1b4f8a);
  border-color: var(--hai-primary, #1b4f8a);
}
.meta {
  flex: 1;
  min-width: 0;
}
.name {
  display: block;
  font-size: 30rpx;
  color: var(--hai-text, #2c2a26);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--hai-text-muted);
}
.ops {
  display: flex;
  gap: 28rpx;
  margin-top: 16rpx;
  justify-content: flex-end;
}
.op {
  font-size: 26rpx;
  color: var(--hai-primary, #1b4f8a);
}
.op.danger {
  color: #cf1322;
}
.batch-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}
.batch-info {
  font-size: 26rpx;
  color: var(--hai-text-muted);
}
.batch-ops {
  display: flex;
  gap: 28rpx;
}
.load-more-wrap {
  width: 100%;
  padding: 16rpx 0 160rpx;
  text-align: center;
}
.load-more-btn {
  display: inline-block;
  padding: 16rpx 48rpx;
  font-size: 26rpx;
  color: var(--hai-primary);
  background: var(--hai-card);
  border-radius: 999rpx;
  box-shadow: var(--hai-shadow);
}
.load-more-tip {
  font-size: 24rpx;
  color: var(--hai-text-muted);
}
</style>
