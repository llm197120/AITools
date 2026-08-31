<route lang="json5">
{ style: { navigationBarTitleText: '搜索文件', navigationBarBackgroundColor: '#F3F2EE', enablePullDownRefresh: true } }
</route>

<template>
  <view class="search-page">
    <view class="search-bar">
      <wd-icon name="search" size="16px" color="#A39E94"></wd-icon>
      <input class="search-input" v-model="keyword" type="text" placeholder="搜索文件名..."
        confirm-type="search" @confirm="doSearch" />
      <text v-if="keyword" class="search-clear" @click="clearSearch">清除</text>
    </view>
    <view v-if="searching" class="results"><HomeSkeleton variant="list" :rows="4" /></view>
    <view v-else class="results">
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
      <HomeEmpty
        v-if="searched && loadFailed"
        title="搜索失败"
        hint="请检查网络后重试"
        action-text="重试"
        :card="true"
        @action="doSearch"
      />
      <HomeEmpty
        v-else-if="searched && results.length === 0"
        title="未找到相关文件"
        hint="换个关键词再试试"
        action-text="清空搜索"
        :card="true"
        @action="clearSearch"
      />
    </view>
    <wd-action-sheet
      v-model="fileSheetVisible"
      :actions="fileSheetActions"
      cancel-text="取消"
      @select="onFileSheetSelect"
    />
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { storageApi } from '../../pages-homeai/api/storage'
import { getStorageDisplayName, normalizeStorageFiles } from '../../pages-homeai/utils/storageFileDisplay'
import { previewFile } from '../../pages-homeai/utils/filePreview'
import { fileSaveActionName } from '../../pages-homeai/utils/contentUrl'
import { downloadStorageFile } from '../../pages-homeai/utils/fileDownload'
import HomeFileIcon from '../../components/HomeFileIcon.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'

useHomeaiPageGuard()
useHomeaiPullRefresh(() => {
  if (keyword.value.trim() && searched.value) return doSearch()
})

const keyword = ref('')
const results = ref<any[]>([])
const searched = ref(false)
const searching = ref(false)
const loadFailed = ref(false)
const fileSheetVisible = ref(false)
const fileSheetActions = ref([
  { name: '预览' },
  { name: '打开文件' },
])
const fileSheetTarget = ref<any>(null)

function clearSearch() {
  keyword.value = ''
  results.value = []
  searched.value = false
  loadFailed.value = false
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  keyword.value = kw
  searched.value = true
  searching.value = true
  loadFailed.value = false
  try {
    results.value = normalizeStorageFiles(await storageApi.search(kw))
  } catch {
    results.value = []
    loadFailed.value = true
  } finally {
    searching.value = false
  }
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
  fileSheetTarget.value = file
  fileSheetActions.value = [{ name: '预览' }, { name: fileSaveActionName(file.extension) }]
  fileSheetVisible.value = true
}

function onFileSheetSelect({ index }: { index: number }) {
  const file = fileSheetTarget.value
  if (!file) return
  if (index === 0) {
    openPreview(file)
    return
  }
  downloadStorageFile({
    id: file.id,
    fileUrl: file.fileUrl,
    originalName: file.originalName,
    extension: file.extension,
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
.search-clear { font-size: 24rpx; color: var(--hai-primary); padding: 8rpx; }
.results { padding: 24rpx 32rpx 48rpx; }
.result-item { display: flex; align-items: center; padding: 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx; gap: 16rpx; box-shadow: var(--hai-shadow); }
.result-icon { font-size: 28rpx; }
.result-name { flex: 1; font-size: 28rpx; color: var(--hai-text); }
.result-meta { font-size: 22rpx; color: var(--hai-text-muted); }
</style>
