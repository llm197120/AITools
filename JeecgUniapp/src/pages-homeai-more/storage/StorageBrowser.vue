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
            <text v-if="folder.userId && folder.userId !== userId" class="tag-other">{{ ownerLabel(folder.userId) }}</text>
          </text>
        </view>
        <text v-if="canEditFolder(folder)" class="action-hint" @click.stop="showFolderActions(folder)">管理</text>
        <wd-icon name="arrow-right" size="14px" color="#A39E94" />
      </view>

      <!-- 文件列表（根目录与子文件夹均展示） -->
      <view
        class="file-item"
        v-for="file in files"
        :key="file.id"
        @click="openFile(file)"
        @longpress="showFileActions(file)"
      >
        <image
          v-if="file.thumbnailUrl"
          class="file-thumb"
          :src="file.thumbnailUrl"
          mode="aspectFill"
          lazy-load
        />
        <HomeFileIcon v-else :ext="file.extension" :name="getStorageDisplayName(file)" />
        <view class="file-info">
          <text class="file-name">{{ getStorageDisplayName(file) }}</text>
          <text class="file-meta">
            {{ formatSize(file.fileSize) }} · {{ formatTime(file.createTime) }}
            <text v-if="!folderId && file.visibility === 'family'" class="tag-family">家庭</text>
            <text v-else-if="!folderId && file.visibility === 'public'" class="tag-public">公开</text>
            <text v-if="file.userId && file.userId !== userId" class="tag-other">{{ ownerLabel(file.userId) }}</text>
          </text>
        </view>
        <text class="action-hint" @click.stop="showFileActions(file)">更多</text>
      </view>

      <view v-if="files.length" class="load-more-wrap">
        <text v-if="loadingMore" class="load-more-text">加载中...</text>
        <view v-else-if="hasMore" class="load-more-btn" @click="loadMore">加载更多</view>
        <text v-else class="load-more-text">没有更多了</text>
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

    <wd-action-sheet
      v-model="folderSheetVisible"
      :actions="folderSheetActions"
      cancel-text="取消"
      @select="onFolderSheetSelect"
    />
    <wd-action-sheet
      v-model="fileSheetVisible"
      :actions="fileSheetActions"
      cancel-text="取消"
      @select="onFileSheetSelect"
    />
    <wd-popup v-model="namePopupVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">{{ namePopupTitle }}</view>
      <view class="dialog-body">
        <wd-input v-model="namePopupValue" :placeholder="namePopupPlaceholder" />
      </view>
      <view class="dialog-footer">
        <wd-button block @click="namePopupVisible = false">取消</wd-button>
        <wd-button type="primary" block @click="submitNamePopup">确定</wd-button>
      </view>
    </wd-popup>
    <wd-popup v-model="confirmVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
      <view class="dialog-title">{{ confirmTitle }}</view>
      <view class="dialog-body">
        <text class="dialog-hint">{{ confirmMessage }}</text>
      </view>
      <view class="dialog-footer">
        <wd-button block @click="confirmVisible = false">取消</wd-button>
        <wd-button type="error" block @click="submitConfirm">确认删除</wd-button>
      </view>
    </wd-popup>
  </view>
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import { useStorageBrowser } from '../../pages-homeai/utils/useStorageBrowser'
import { useMemberLabel } from '../../pages-homeai/utils/useMemberLabel'
import HomeSkeleton from '../../components/HomeSkeleton.vue'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { getStorageDisplayName } from '../../pages-homeai/utils/storageFileDisplay'
import HomeFileIcon from '../../components/HomeFileIcon.vue'

const props = defineProps<{ folderId: string | null; userId?: string }>()

const { loadMemberLabels, resolveMemberLabel } = useMemberLabel()

const {
  files,
  loading,
  subFolders,
  breadcrumbs,
  refresh,
  loadMore,
  hasMore,
  loadingMore,
  enterFolder,
  onBreadcrumb,
  openFile,
  createFolder,
  uploadFiles,
  showFolderActions,
  showFileActions,
  canEditFolder,
  folderSheetVisible,
  folderSheetActions,
  onFolderSheetSelect,
  fileSheetVisible,
  fileSheetActions,
  onFileSheetSelect,
  namePopupVisible,
  namePopupTitle,
  namePopupPlaceholder,
  namePopupValue,
  submitNamePopup,
  confirmVisible,
  confirmTitle,
  confirmMessage,
  submitConfirm,
} = useStorageBrowser(
  () => props.folderId,
  () => props.userId,
)

defineExpose({ refresh, loadMore })

function ownerLabel(uid: string) {
  return resolveMemberLabel(uid, '家庭成员')
}

onMounted(async () => {
  // 首次进入由父页面 onShow 触发 refresh，此处仅加载成员标签避免重复拉取
  await loadMemberLabels()
})

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
.browser { min-height: 100vh; background: var(--hai-bg); padding: 16rpx 32rpx 160rpx; box-sizing: border-box; }
.breadcrumb {
  white-space: nowrap;
  padding: 20rpx 24rpx;
  background: var(--hai-card);
  border-radius: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: var(--hai-shadow);
}
.crumb { font-size: 26rpx; color: var(--hai-primary); }
.crumb.active { color: var(--hai-text); font-weight: 600; }
.sep { color: #c4bfb6; }
.loading-wrap { padding: 20rpx; }
.folder-item, .file-item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 28rpx 24rpx; background: var(--hai-card); border-radius: 24rpx; margin-bottom: 12rpx;
  box-shadow: var(--hai-shadow);
}
.folder-icon { font-size: 36rpx; }
.file-thumb {
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #ece9e2;
}
.folder-info, .file-info { flex: 1; min-width: 0; }
.load-more-wrap {
  padding: 24rpx 0 8rpx;
  display: flex;
  justify-content: center;
}
.load-more-text { font-size: 24rpx; color: var(--hai-text-muted); }
.load-more-btn {
  font-size: 26rpx;
  color: var(--hai-primary);
  padding: 12rpx 32rpx;
}
.folder-name, .file-name { font-size: 28rpx; color: var(--hai-text); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.folder-meta, .file-meta { font-size: 22rpx; color: var(--hai-text-muted); display: block; margin-top: 4rpx; }
.tag-family { color: var(--hai-success); margin-left: 8rpx; }
.tag-public { color: var(--hai-primary); margin-left: 8rpx; }
.tag-other { color: var(--hai-danger); margin-left: 8rpx; }
.action-hint { font-size: 22rpx; color: var(--hai-primary); padding: 8rpx 12rpx; flex-shrink: 0; }
.fab-group {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; gap: 20rpx; padding: 20rpx 30rpx calc(40rpx + env(safe-area-inset-bottom));
  background: linear-gradient(transparent, var(--hai-bg) 30%);
  justify-content: center;
}
.fab { padding: 20rpx 36rpx; background: var(--hai-card); border-radius: 999rpx; font-size: 26rpx; color: var(--hai-text); box-shadow: var(--hai-shadow); }
.fab.primary { background: var(--hai-primary); color: var(--hai-on-primary); box-shadow: 0 8rpx 28rpx rgba(27, 79, 138, 0.28); }
.dialog-title {
  font-family: var(--hai-serif);
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  padding: 36rpx 24rpx 10rpx;
  color: var(--hai-text);
}
.dialog-body { padding: 20rpx 30rpx; }
.dialog-hint { display: block; font-size: 26rpx; color: var(--hai-text-secondary); line-height: 1.5; }
.dialog-footer { display: flex; gap: 20rpx; padding: 0 30rpx 30rpx; }
</style>
