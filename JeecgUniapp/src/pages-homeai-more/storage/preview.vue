<route lang="json5">{ style: { navigationBarTitleText: '文件预览', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="preview-page">
    <view v-if="loading" class="loading">加载中...</view>
    <template v-else>
      <text class="file-name">{{ fileName }}</text>

      <!-- 图片 -->
      <image
        v-if="mode === 'image'"
        class="preview-image"
        :src="fileUrl"
        mode="widthFix"
        show-menu-by-longpress
        @click="previewFullImage"
      />

      <!-- 视频 -->
      <video v-else-if="mode === 'video'" class="preview-video" :src="fileUrl" controls />

      <!-- 文本 -->
      <scroll-view v-else-if="mode === 'text'" scroll-y class="text-box">
        <text class="text-content" selectable>{{ textContent }}</text>
      </scroll-view>

      <!-- PDF / 其他文档 -->
      <view v-else class="doc-box">
        <text class="doc-icon">📄</text>
        <text class="doc-tip">{{ mode === 'pdf' ? 'PDF 文档' : '文档文件' }}</text>
        <wd-button type="primary" @click="openDocument">打开文档</wd-button>
      </view>

      <view class="action-bar">
        <wd-button v-if="mode === 'image'" type="primary" block @click="handleSaveImage">
          保存到相册
        </wd-button>
        <wd-button v-else-if="mode === 'video'" type="primary" block @click="handleDownload">
          保存视频到相册
        </wd-button>
        <wd-button v-else type="primary" block @click="handleDownload">
          下载文件
        </wd-button>
      </view>
    </template>
  </view>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storageApi } from '../../pages-homeai/api/storage'
import { downloadStorageFile, saveStorageImage } from '../../pages-homeai/utils/fileDownload'
import { getStorageDisplayName, normalizeStorageFile } from '../../pages-homeai/utils/storageFileDisplay'
import {
  getFileExt,
  isImageExt,
  isVideoExt,
  isPdfExt,
  isTextExt,
  previewImageUrl,
} from '../../pages-homeai/utils/filePreview'

const loading = ref(true)
const fileId = ref('')
const fileUrl = ref('')
const fileName = ref('')
const fileExt = ref('')
const mode = ref<'image' | 'video' | 'text' | 'pdf' | 'other'>('other')
const textContent = ref('')
const tempFilePath = ref('')

function detectMode(name: string, ext?: string) {
  const e = ext || getFileExt(name)
  if (isImageExt(e)) return 'image'
  if (isVideoExt(e)) return 'video'
  if (isTextExt(e)) return 'text'
  if (isPdfExt(e)) return 'pdf'
  return 'other'
}

function fileInput() {
  return {
    id: fileId.value || undefined,
    fileUrl: fileUrl.value,
    originalName: fileName.value,
    extension: fileExt.value,
  }
}

async function loadTextContent(url: string) {
  return new Promise<void>((resolve, reject) => {
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error('下载失败'))
          return
        }
        tempFilePath.value = res.tempFilePath
        // H5 无文件系统 API，优雅降级提示（文本预览为小程序能力）
        const fsm: any = uni.getFileSystemManager?.()
        if (!fsm) {
          textContent.value = '当前平台不支持文本预览'
          resolve()
          return
        }
        fsm.readFile({
          filePath: res.tempFilePath,
          encoding: 'utf-8',
          success: (r: any) => {
            textContent.value = typeof r.data === 'string' ? r.data : String(r.data)
            resolve()
          },
          fail: reject,
        })
      },
      fail: reject,
    })
  })
}

function previewFullImage() {
  if (fileUrl.value) previewImageUrl(fileUrl.value)
}

function openDocument() {
  const open = (path: string) => {
    uni.openDocument({
      filePath: path,
      showMenu: true,
      fail: () => uni.showToast({ title: '无法打开该文件', icon: 'none' }),
    })
  }
  if (tempFilePath.value) {
    open(tempFilePath.value)
    return
  }
  handleDownload()
}

async function handleSaveImage() {
  await saveStorageImage(fileInput())
}

async function handleDownload() {
  await downloadStorageFile(fileInput())
}

onLoad(async (opts: any) => {
  try {
    if (opts?.fileId) {
      fileId.value = opts.fileId
      const file = normalizeStorageFile(await storageApi.fileDetail(opts.fileId))
      fileUrl.value = file.fileUrl || ''
      fileName.value = getStorageDisplayName(file)
      fileExt.value = file.extension || getFileExt(fileName.value)
      mode.value = detectMode(fileName.value, fileExt.value)
    } else if (opts?.url) {
      fileUrl.value = decodeURIComponent(opts.url)
      fileName.value = decodeURIComponent(opts.name || '文件')
      fileExt.value = opts.ext ? decodeURIComponent(opts.ext) : getFileExt(fileName.value)
      mode.value = detectMode(fileName.value, fileExt.value)
    }
    uni.setNavigationBarTitle({ title: fileName.value.substring(0, 12) })
    if (mode.value === 'text' && fileUrl.value) {
      await loadTextContent(fileUrl.value)
    } else if ((mode.value === 'pdf' || mode.value === 'other') && fileUrl.value) {
      uni.downloadFile({
        url: fileUrl.value,
        success: (res) => {
          if (res.statusCode === 200) tempFilePath.value = res.tempFilePath
        },
      })
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.preview-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 160rpx; box-sizing: border-box; }
.loading { text-align: center; padding: 80rpx; color: var(--hai-text-muted); }
.file-name { font-size: 28rpx; color: var(--hai-text-secondary); display: block; margin-bottom: 20rpx; word-break: break-all; }
.preview-image { width: 100%; border-radius: 24rpx; background: var(--hai-card); box-shadow: var(--hai-shadow); }
.preview-video { width: 100%; height: 420rpx; border-radius: 24rpx; background: #000; }
.text-box { height: calc(100vh - 280rpx); background: var(--hai-card); border-radius: 24rpx; padding: 24rpx; box-sizing: border-box; box-shadow: var(--hai-shadow); }
.text-content { font-size: 26rpx; color: var(--hai-text); line-height: 1.7; white-space: pre-wrap; word-break: break-all; }
.doc-box { background: var(--hai-card); border-radius: 28rpx; padding: 80rpx 40rpx; text-align: center; box-shadow: var(--hai-shadow); }
.doc-icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
.doc-tip { font-size: 28rpx; color: var(--hai-text-secondary); display: block; margin-bottom: 40rpx; }
.action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: var(--hai-card); border-top: 1rpx solid var(--hai-border);
}
</style>
