<route lang="json5">
{ style: { navigationBarTitleText: '搜索文件', navigationBarBackgroundColor: '#F3F2EE' } }
</route>

<template>
  <view class="search-page">
    <view class="search-bar">
      <wd-icon name="search" size="16px" color="#A39E94"></wd-icon>
      <input class="search-input" v-model="keyword" type="text" placeholder="搜索文件名..."
        confirm-type="search" @confirm="doSearch" />
    </view>
    <view class="results">
      <view
        class="result-item"
        v-for="file in results"
        :key="file.id"
        @click="openPreview(file)"
        @longpress="downloadFile(file)"
      >
        <HomeFileIcon :ext="file.extension" :name="getStorageDisplayName(file)" />
        <text class="result-name">{{ getStorageDisplayName(file) }}</text>
        <text class="result-meta">{{ formatSize(file.fileSize) }}</text>
      </view>
      <HomeEmpty v-if="searched && results.length === 0" title="未找到相关文件" hint="换个关键词再试试" :card="true" />
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { storageApi } from '../../pages-homeai/api/storage'
import { getStorageDisplayName, normalizeStorageFiles } from '../../pages-homeai/utils/storageFileDisplay'
import { previewFile } from '../../pages-homeai/utils/filePreview'
import { downloadStorageFile } from '../../pages-homeai/utils/fileDownload'
import HomeFileIcon from '../../components/HomeFileIcon.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'

const keyword = ref('')
const results = ref<any[]>([])
const searched = ref(false)

async function doSearch() {
  searched.value = true
  results.value = normalizeStorageFiles(await storageApi.search(keyword.value))
}

function openPreview(file: any) {
  previewFile({
    id: file.id,
    fileUrl: file.fileUrl,
    originalName: file.originalName,
    extension: file.extension,
  })
}

function downloadFile(file: any) {
  uni.showActionSheet({
    itemList: ['下载', '预览'],
    success: (res) => {
      if (res.tapIndex === 0) {
        downloadStorageFile({
          id: file.id,
          fileUrl: file.fileUrl,
          originalName: file.originalName,
          extension: file.extension,
        })
      } else {
        openPreview(file)
      }
    },
  })
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}
</script>

<style scoped>
.search-page { min-height: 100vh; background: var(--hai-bg); }
.search-bar { display: flex; align-items: center; padding: 16rpx 32rpx; background: var(--hai-card); gap: 12rpx; border-bottom: 1rpx solid var(--hai-border); }
.search-input { flex: 1; height: 68rpx; padding: 0 20rpx; background: var(--hai-bg); border-radius: 34rpx; font-size: 28rpx; color: var(--hai-text); }
.results { padding: 24rpx 32rpx 48rpx; }
.result-item { display: flex; align-items: center; padding: 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; gap: 16rpx; box-shadow: var(--hai-shadow); }
.result-icon { font-size: 28rpx; }
.result-name { flex: 1; font-size: 28rpx; color: var(--hai-text); }
.result-meta { font-size: 22rpx; color: var(--hai-text-muted); }
</style>
