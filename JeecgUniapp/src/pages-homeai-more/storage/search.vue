<route lang="json5">
{ style: { navigationBarTitleText: '搜索文件' } }
</route>

<template>
  <view class="search-page">
    <view class="search-bar">
      <wd-icon name="search" size="16px" color="#999"></wd-icon>
      <input class="search-input" v-model="keyword" type="text" placeholder="搜索文件名..."
        confirm-type="search" @confirm="doSearch" />
    </view>
    <view class="results">
      <view class="result-item" v-for="file in results" :key="file.id" @click="previewFile(file)">
        <text class="result-icon">{{ file.icon }}</text>
        <text class="result-name">{{ file.originalName }}</text>
        <text class="result-meta">{{ formatSize(file.fileSize) }}</text>
      </view>
      <view v-if="searched && results.length === 0" class="empty"><text>未找到相关文件</text></view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { get as getApi } from '../../pages-homeai/api/request'

const keyword = ref('')
const results = ref<any[]>([])
const searched = ref(false)

async function doSearch() {
  searched.value = true
  results.value = await getApi('/storage/files/search', { params: { keyword: keyword.value } })
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}

function previewFile(file: any) { uni.showToast({ title: '预览: ' + file.originalName, icon: 'none' }) }
</script>

<style scoped>
.search-page { min-height: 100vh; background: #f5f5f5; }
.search-bar { display: flex; align-items: center; padding: 16rpx 30rpx; background: #fff; gap: 12rpx; border-bottom: 1rpx solid #eee; }
.search-input { flex: 1; height: 68rpx; padding: 0 20rpx; background: #f5f5f5; border-radius: 34rpx; font-size: 28rpx; }
.results { padding: 20rpx; }
.result-item { display: flex; align-items: center; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; gap: 16rpx; }
.result-icon { font-size: 28rpx; }
.result-name { flex: 1; font-size: 28rpx; color: #333; }
.result-meta { font-size: 22rpx; color: #999; }
.empty { text-align: center; padding: 80rpx 0; color: #999; }
</style>
