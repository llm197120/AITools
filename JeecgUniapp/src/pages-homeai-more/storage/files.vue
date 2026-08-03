<route lang="json5">
{ style: { navigationBarTitleText: '文件列表' } }
</route>

<template>
  <view class="files-page">
    <view class="file-item" v-for="file in files" :key="file.id" @longpress="showFileAction(file)">
      <view class="file-icon">{{ getIcon(file.extension) }}</view>
      <view class="file-info">
        <text class="file-name">{{ file.originalName }}</text>
        <text class="file-meta">{{ formatSize(file.fileSize) }} · {{ formatTime(file.createTime) }}
          <text v-if="file.downloadCount > 0"> · 下载 {{ file.downloadCount }} 次</text>
        </text>
      </view>
      <view class="file-tags">
        <text v-if="file.isFavorite === '1'" class="tag-fav">⭐</text>
        <text v-if="isNew(file.createTime)" class="tag-new">NEW</text>
      </view>
    </view>

    <view v-if="files.length === 0" class="empty">
      <text>暂无文件，点击右下角上传</text>
    </view>

    <view class="fab" @click="showUploadMenu"><text>+</text></view>

    <wd-action-sheet v-model="actionShow" :actions="fileActions" @select="onFileAction"></wd-action-sheet>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { get as getApi, put as putApi, del as delApi } from '../../pages-homeai/api/request'

const files = ref<any[]>([])
const actionShow = ref(false)
const selectedFile = ref<any>(null)
const fileActions = ref([
  { name: '收藏/取消', key: 'favorite' },
  { name: '格式转换', key: 'convert' },
  { name: '删除', key: 'delete', color: '#e74c3c' },
])

const folderId = ''

onLoad((options: any) => {
  loadFiles(options?.folderId || '')
})

async function loadFiles(fid: string) {
  files.value = await getApi(`/storage/folders/${fid}/files`)
}

function getIcon(ext: string) {
  const map: Record<string, string> = { pdf: '📄', doc: '📝', docx: '📝', xls: '📊', xlsx: '📊', ppt: '📽️', pptx: '📽️', jpg: '🖼️', png: '🖼️', gif: '🖼️', mp4: '🎬', zip: '📦', rar: '📦', txt: '📃', md: '📃' }
  return map[ext] || '📎'
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}

function formatTime(t: string) { return t ? t.substring(0, 10) : '' }
function isNew(t: string) { return t && Date.now() - new Date(t).getTime() < 86400000 }

function showFileAction(file: any) { selectedFile.value = file; actionShow.value = true }

function showUploadMenu() {
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择', '选择文件'],
    success: (res) => {
      const types: any[] = [['camera'], ['album'], ['all']]
      uni.chooseImage({ count: 1, sourceType: types[res.tapIndex] || ['album'] })
    },
  })
}

async function onFileAction(e: any) {
  const f = selectedFile.value
  if (!f) return
  if (e.key === 'favorite') { await putApi(`/storage/files/${f.id}/favorite`); await loadFiles('') }
  else if (e.key === 'convert') { uni.navigateTo({ url: `/pages/homeai-more/storage/office-convert?fileId=${f.id}&format=${f.extension}` }) }
  else if (e.key === 'delete') {
    uni.showModal({
      title: '删除文件', content: '确定删除吗？',
      success: async (r) => { if (r.confirm) { await delApi(`/storage/files/${f.id}`); await loadFiles('') } },
    })
  }
}
</script>

<style scoped>
.files-page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; }
.file-item { display: flex; align-items: center; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; gap: 16rpx; }
.file-icon { font-size: 36rpx; width: 60rpx; text-align: center; }
.file-info { flex: 1; }
.file-name { font-size: 28rpx; color: #333; display: block; }
.file-meta { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.tag-fav { font-size: 20rpx; }
.tag-new { font-size: 18rpx; background: #e74c3c; color: #fff; padding: 2rpx 10rpx; border-radius: 6rpx; }
.empty { text-align: center; padding: 100rpx 0; color: #999; }
.fab { position: fixed; right: 40rpx; bottom: 100rpx; width: 100rpx; height: 100rpx; background: #667eea; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48rpx; color: #fff; box-shadow: 0 4rpx 20rpx rgba(102,126,234,0.4); }
</style>
