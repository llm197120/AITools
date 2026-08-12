<route lang="json5">
{ style: { navigationBarTitleText: '回收站', navigationBarBackgroundColor: '#F3F2EE' } }
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
      <HomeEmpty v-if="!loading && rows.length === 0" title="回收站是空的" hint="删除的文件会出现在这里" />
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
import { onShow } from '@dcloudio/uni-app'
import { storageApi } from '../../pages-homeai/api/storage'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'

useHomeaiPageGuard()

const type = ref<'file' | 'folder'>('file')
const rows = ref<any[]>([])
const selected = ref<string[]>([])
const loading = ref(false)

onShow(() => {
  load()
})

function switchType(t: 'file' | 'folder') {
  type.value = t
  selected.value = []
  load()
}

async function load() {
  loading.value = true
  try {
    const res: any = await storageApi.myRecycleBin({
      type: type.value,
      pageNo: 1,
      pageSize: 100,
    })
    rows.value = res?.records || []
    selected.value = []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
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
  await storageApi.myRestore(payload([id]))
  uni.showToast({ title: '已恢复', icon: 'success' })
  await load()
}

async function purgeOne(id: string) {
  uni.showModal({
    title: '彻底删除',
    content: '删除后不可恢复，确定？',
    success: async (r) => {
      if (!r.confirm) return
      await storageApi.myDeletePermanently(payload([id]))
      uni.showToast({ title: '已删除', icon: 'success' })
      await load()
    },
  })
}

async function restoreSelected() {
  if (!selected.value.length) return
  await storageApi.myRestore(payload(selected.value))
  uni.showToast({ title: '已恢复', icon: 'success' })
  await load()
}

async function purgeSelected() {
  if (!selected.value.length) return
  uni.showModal({
    title: '彻底删除',
    content: `确定彻底删除选中的 ${selected.value.length} 项？`,
    success: async (r) => {
      if (!r.confirm) return
      await storageApi.myDeletePermanently(payload(selected.value))
      uni.showToast({ title: '已删除', icon: 'success' })
      await load()
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
</style>
