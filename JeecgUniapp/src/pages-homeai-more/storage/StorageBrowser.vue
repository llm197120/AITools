<template>
  <view class="browser">
    <!-- 面包屑 -->
    <scroll-view scroll-x class="breadcrumb">
      <text
        v-for="(c, i) in breadcrumbs"
        :key="c.id || 'root'"
        class="crumb"
        :class="{ active: i === breadcrumbs.length - 1 }"
        @click="onBreadcrumb(c.id)"
      >
        {{ c.name }}<text v-if="i < breadcrumbs.length - 1" class="sep"> / </text>
      </text>
    </scroll-view>

    <view v-if="loading" class="loading-wrap"><HomeSkeleton variant="list" :rows="5" /></view>

    <template v-else>
      <!-- 子文件夹 -->
      <view
        class="folder-item"
        v-for="folder in subFolders"
        :key="folder.id"
        @click="enterFolder(folder)"
        @longpress="showFolderActions(folder)"
      >
        <text class="folder-icon">📂</text>
        <view class="folder-info">
          <text class="folder-name">{{ folder.name }}</text>
          <text class="folder-meta">
            {{ folder.fileCount || 0 }} 个文件
            <text v-if="folder.visibility === 'family'" class="tag-family">家庭</text>
            <text v-else-if="folder.visibility === 'public'" class="tag-public">公开</text>
            <text v-if="folder.userId && folder.userId !== userId" class="tag-other">他人创建</text>
          </text>
        </view>
        <text v-if="canEditFolder(folder)" class="action-hint" @click.stop="showFolderActions(folder)">管理</text>
        <wd-icon name="arrow-right" size="14px" color="#ccc" />
      </view>

      <!-- 文件列表（根目录与子文件夹均展示） -->
      <view
        class="file-item"
        v-for="file in files"
        :key="file.id"
        @click="openFile(file)"
        @longpress="showFileActions(file)"
      >
        <HomeFileIcon :ext="file.extension" :name="getStorageDisplayName(file)" />
        <view class="file-info">
          <text class="file-name">{{ getStorageDisplayName(file) }}</text>
          <text class="file-meta">
            {{ formatSize(file.fileSize) }} · {{ formatTime(file.createTime) }}
            <text v-if="!folderId && file.visibility === 'family'" class="tag-family">家庭</text>
            <text v-else-if="!folderId && file.visibility === 'public'" class="tag-public">公开</text>
          </text>
        </view>
        <text class="action-hint" @click.stop="showFileActions(file)">更多</text>
      </view>

      <HomeEmpty
        v-if="!subFolders.length && !files.length"
        :title="folderId ? '文件夹为空' : '暂无资料'"
        :hint="folderId ? '点击下方按钮上传文件' : '可新建文件夹，或直接上传文件到根目录'"
        action-text="新建文件夹"
        @action="createFolder(folderId)"
      />
    </template>

    <!-- 底部操作 -->
    <view class="fab-group">
      <view class="fab" @click="createFolder(folderId)">📁 新建</view>
      <view class="fab primary" @click="uploadFiles">📤 上传</view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import { useStorageBrowser } from '../../pages-homeai/utils/useStorageBrowser'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { getStorageDisplayName, normalizeStorageFiles } from '../../pages-homeai/utils/storageFileDisplay'
import HomeFileIcon from '../../components/HomeFileIcon.vue'

const props = defineProps<{ folderId: string | null; userId?: string }>()

const {
  files,
  loading,
  subFolders,
  breadcrumbs,
  refresh,
  enterFolder,
  onBreadcrumb,
  openFile,
  createFolder,
  uploadFiles,
  showFolderActions,
  showFileActions,
  canEditFolder,
} = useStorageBrowser(
  () => props.folderId,
  () => props.userId,
)

defineExpose({ refresh })

onMounted(() => refresh())

function formatSize(bytes: number) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}

function formatTime(t: string) {
  return t ? t.substring(0, 10) : ''
}
</script>

<style scoped>
.browser { min-height: 100vh; background: #f5f5f5; padding-bottom: 160rpx; }
.breadcrumb { white-space: nowrap; padding: 20rpx 24rpx; background: #fff; border-bottom: 1rpx solid #eee; }
.crumb { font-size: 26rpx; color: #667eea; }
.crumb.active { color: #333; font-weight: 600; }
.sep { color: #ccc; }
.loading-wrap { padding: 20rpx; }
.folder-item, .file-item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 28rpx 24rpx; background: #fff; border-bottom: 1rpx solid #f5f5f5;
}
.folder-icon { font-size: 36rpx; }
.folder-info, .file-info { flex: 1; min-width: 0; }
.folder-name, .file-name { font-size: 28rpx; color: #333; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.folder-meta, .file-meta { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; }
.tag-family { color: #27ae60; margin-left: 8rpx; }
.tag-public { color: #3498db; margin-left: 8rpx; }
.tag-other { color: #e67e22; margin-left: 8rpx; }
.action-hint { font-size: 22rpx; color: #667eea; padding: 8rpx 12rpx; flex-shrink: 0; }
.fab-group {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; gap: 20rpx; padding: 20rpx 30rpx 40rpx;
  background: linear-gradient(transparent, #f5f5f5 30%);
  justify-content: center;
}
.fab { padding: 20rpx 36rpx; background: #fff; border-radius: 40rpx; font-size: 26rpx; color: #333; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08); }
.fab.primary { background: #667eea; color: #fff; }
</style>
