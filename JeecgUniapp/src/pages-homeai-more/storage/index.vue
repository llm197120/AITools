<route lang="json5">
{ style: { navigationBarTitleText: '资料存储' } }
</route>

<template>
  <view class="storage-page">
    <view class="header-bar">
      <wd-icon name="search" size="20px" @click="goSearch"></wd-icon>
      <text class="header-title">我的资料库</text>
      <wd-icon name="add" size="20px" @click="showMenu"></wd-icon>
    </view>

    <view class="folder-list">
      <view class="folder-item" v-for="folder in folders" :key="folder.id"
        @click="goFiles(folder.id)">
        <wd-icon name="folder" size="22px" color="#faad14"></wd-icon>
        <text class="folder-name">{{ folder.name }}</text>
        <text class="folder-count">{{ folder.fileCount || 0 }}个文件</text>
        <text class="folder-visibility">{{ folder.visibility === 'family' ? '家庭' : '私有' }}</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc"></wd-icon>
      </view>
    </view>

    <view class="fab-group">
      <view class="fab" @click="showNewFolder"><text>📁 新建文件夹</text></view>
      <view class="fab" @click="showUpload"><text>📤 上传文件</text></view>
    </view>

    <wd-action-sheet v-model="menuShow" :actions="menuActions" @select="onMenuSelect"></wd-action-sheet>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get as getApi, post as postApi, getServerBaseUrl } from '../../pages-homeai/api/request'

const folders = ref<any[]>([])
const menuShow = ref(false)
const menuActions = ref([
  { name: '新建文件夹', key: 'newFolder' },
  { name: '上传文件', key: 'upload' },
])

onShow(loadFolders)

async function loadFolders() {
  folders.value = await getApi('/storage/folders')
}

function goFiles(folderId: string) {
  uni.navigateTo({ url: `/pages-homeai-more/storage/files?folderId=${folderId}` })
}

function goSearch() {
  uni.navigateTo({ url: '/pages-homeai-more/storage/search' })
}

function showMenu() { menuShow.value = true }

async function onMenuSelect(e: any) {
  if (e.key === 'newFolder') showNewFolder()
  else if (e.key === 'upload') showUpload()
}

function showNewFolder() {
  uni.showModal({
    title: '新建文件夹',
    editable: true,
    placeholderText: '输入文件夹名称',
    success: async (res) => {
      if (res.confirm && res.content) {
        await postApi('/storage/folders', { params: { name: res.content } })
        await loadFolders()
      }
    },
  })
}

function showUpload() {
  uni.chooseImage({
    count: 1,
    success: async (r) => {
      if (!r.tempFilePaths || !r.tempFilePaths[0]) return
      uni.showLoading({ title: '上传中...' })
      try {
        const token = uni.getStorageSync('homeai_token')
        const res: any = await uni.uploadFile({
          url: getServerBaseUrl() + '/homeai/storage/files/upload',
          filePath: r.tempFilePaths[0],
          name: 'file',
          formData: { folderId: '', visibility: 'private' },
          header: { 'X-Access-Token': token },
        })
        const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        if (!data.success) throw new Error(data.message || '上传失败')
        uni.hideLoading()
        uni.showToast({ title: '上传成功', icon: 'success' })
        await loadFolders()
      } catch (e: any) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      }
    },
  })
}
</script>

<style scoped>
.storage-page { min-height: 100vh; background: #f5f5f5; }
.header-bar { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 30rpx; background: #fff; }
.header-title { font-size: 32rpx; font-weight: 600; }
.folder-list { padding: 20rpx; }
.folder-item { display: flex; align-items: center; padding: 28rpx 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 12rpx; gap: 16rpx; }
.folder-name { flex: 1; font-size: 28rpx; color: #333; }
.folder-count { font-size: 22rpx; color: #999; }
.folder-visibility { font-size: 20rpx; color: #667eea; background: #f0f0ff; padding: 2rpx 12rpx; border-radius: 8rpx; }
.fab-group { display: flex; gap: 20rpx; padding: 20rpx 30rpx; justify-content: center; }
.fab { padding: 20rpx 40rpx; background: #667eea; border-radius: 40rpx; color: #fff; font-size: 26rpx; }
</style>
