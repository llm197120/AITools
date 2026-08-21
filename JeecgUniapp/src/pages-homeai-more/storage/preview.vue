<route lang="json5">{ style: { navigationBarTitleText: '文件预览', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="preview-page">
    <view v-if="loading" class="loading">加载中...</view>
    <template v-else>
      <text class="file-name">{{ fileName }}</text>
      <text v-if="convertHint" class="convert-hint">{{ convertHint }}</text>

      <image
        v-if="mode === 'image'"
        class="preview-image"
        :src="fileUrl"
        mode="widthFix"
        show-menu-by-longpress
        @click="previewFullImage"
      />

      <video v-else-if="mode === 'video'" class="preview-video" :src="fileUrl" controls />

      <NativeHtmlAudio v-else-if="mode === 'audio'" class="preview-audio" :src="fileUrl" />

      <scroll-view v-else-if="mode === 'text'" scroll-y class="text-box">
        <text class="text-content" selectable>{{ textContent }}</text>
      </scroll-view>

      <view v-else-if="mode === 'pdf'" class="pdf-box">
        <canvas id="homeai-pdf-canvas" class="pdf-canvas" />
        <view v-if="pdfTotal > 1" class="pdf-nav">
          <wd-button size="small" :disabled="pdfPage <= 1" @click="changePdfPage(-1)">上一页</wd-button>
          <text class="pdf-page">{{ pdfPage }} / {{ pdfTotal }}</text>
          <wd-button size="small" :disabled="pdfPage >= pdfTotal" @click="changePdfPage(1)">下一页</wd-button>
        </view>
        <wd-button v-if="pdfFailed" type="primary" @click="openDocument">系统打开</wd-button>
      </view>

      <view v-else class="doc-box">
        <text class="doc-icon">📄</text>
        <text class="doc-tip">{{ mode === 'office' ? 'Office 文档' : '文档文件' }}</text>
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
import { nextTick, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NativeHtmlAudio from '../../pages-homeai/components/NativeHtmlAudio'
import { storageApi } from '../../pages-homeai/api/storage'
import { learnApi } from '../../pages-homeai/api/learn'
import { downloadStorageFile, saveStorageImage } from '../../pages-homeai/utils/fileDownload'
import { getStorageDisplayName, normalizeStorageFile } from '../../pages-homeai/utils/storageFileDisplay'
import { openLocalDocument } from '../../pages-homeai/platform/download'
import { fetchPdfBuffer, renderPdfPage } from '../../pages-homeai/utils/pdfPreview'
import {
  getFileExt,
  isAudioExt,
  isImageExt,
  isOfficeExt,
  isPdfExt,
  isTextExt,
  isVideoExt,
  previewImageUrl,
} from '../../pages-homeai/utils/filePreview'

type PreviewMode = 'image' | 'video' | 'audio' | 'text' | 'pdf' | 'office' | 'other'

const loading = ref(true)
const fileId = ref('')
const materialId = ref('')
const fileUrl = ref('')
const fileName = ref('')
const fileExt = ref('')
const mode = ref<PreviewMode>('other')
const textContent = ref('')
const tempFilePath = ref('')
const convertHint = ref('')
const pdfPage = ref(1)
const pdfTotal = ref(1)
const pdfFailed = ref(false)
let pdfBuffer: ArrayBuffer | null = null
let pollTimer: ReturnType<typeof setInterval> | null = null

function detectMode(name: string, ext?: string, kind?: string): PreviewMode {
  if (kind === 'image' || kind === 'video' || kind === 'audio' || kind === 'text' || kind === 'pdf' || kind === 'office') {
    return kind
  }
  const e = ext || getFileExt(name)
  if (isImageExt(e)) return 'image'
  if (isVideoExt(e)) return 'video'
  if (isAudioExt(e)) return 'audio'
  if (isTextExt(e)) return 'text'
  if (isPdfExt(e)) return 'pdf'
  if (isOfficeExt(e)) return 'office'
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

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function loadTextContent(url: string) {
  try {
    if (typeof fetch === 'function') {
      const res = await fetch(url)
      textContent.value = await res.text()
      return
    }
  } catch {
    // 回退下载
  }
  return new Promise<void>((resolve, reject) => {
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error('下载失败'))
          return
        }
        tempFilePath.value = res.tempFilePath
        const fsm: any = uni.getFileSystemManager?.()
        if (!fsm) {
          textContent.value = '当前平台不支持文本预览，请下载查看'
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

async function showPdf(url: string) {
  pdfFailed.value = false
  try {
    pdfBuffer = await fetchPdfBuffer(url)
    await nextTick()
    const canvas = document.getElementById('homeai-pdf-canvas') as HTMLCanvasElement | null
    if (!canvas || !pdfBuffer) {
      pdfFailed.value = true
      return
    }
    pdfTotal.value = await renderPdfPage(pdfBuffer, canvas, pdfPage.value)
  } catch {
    pdfFailed.value = true
    convertHint.value = '页内预览失败，可尝试系统打开'
  }
}

async function changePdfPage(delta: number) {
  if (!pdfBuffer) return
  const next = pdfPage.value + delta
  if (next < 1 || next > pdfTotal.value) return
  pdfPage.value = next
  const canvas = document.getElementById('homeai-pdf-canvas') as HTMLCanvasElement | null
  if (canvas) await renderPdfPage(pdfBuffer, canvas, pdfPage.value)
}

function applyKind(kind?: string, previewPdfUrl?: string) {
  mode.value = detectMode(fileName.value, fileExt.value, kind)
  if (mode.value === 'office' && previewPdfUrl) {
    fileUrl.value = previewPdfUrl
    mode.value = 'pdf'
  }
}

async function pollPreview(getPreview: () => Promise<any>) {
  stopPoll()
  convertHint.value = '正在转换为 PDF…'
  pollTimer = setInterval(async () => {
    try {
      const data = await getPreview()
      if (data?.previewPdfUrl) {
        stopPoll()
        convertHint.value = ''
        fileUrl.value = data.previewPdfUrl
        mode.value = 'pdf'
        await showPdf(data.previewPdfUrl)
      } else if (data?.convertStatus === 'FAILED') {
        stopPoll()
        convertHint.value = data.errorMessage || '转换失败，请下载或系统打开'
        mode.value = 'office'
      }
    } catch {
      stopPoll()
    }
  }, 3000)
}

async function ensureOfficePdf(getPreview: () => Promise<any>, startConvert: () => Promise<any>) {
  const data = await getPreview()
  applyKind(data?.kind, data?.previewPdfUrl)
  if (data?.fileUrl) fileUrl.value = data.fileUrl
  if (mode.value === 'pdf' && data?.previewPdfUrl) {
    await showPdf(data.previewPdfUrl)
    return
  }
  if (mode.value !== 'office') return
  const started = await startConvert()
  if (started?.previewPdfUrl) {
    fileUrl.value = started.previewPdfUrl
    mode.value = 'pdf'
    await showPdf(started.previewPdfUrl)
    return
  }
  await pollPreview(getPreview)
}

function previewFullImage() {
  if (fileUrl.value) previewImageUrl(fileUrl.value)
}

function openDocument() {
  const open = (path: string) => openLocalDocument(path)
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
      await ensureOfficePdf(
        () => storageApi.preview(fileId.value),
        () => storageApi.previewPdf(fileId.value),
      )
      if (mode.value === 'other' || !opts) {
        mode.value = detectMode(fileName.value, fileExt.value)
      }
    } else if (opts?.materialId) {
      materialId.value = opts.materialId
      await ensureOfficePdf(
        () => learnApi.preview(materialId.value),
        () => learnApi.previewPdf(materialId.value),
      )
      if (!fileName.value) fileName.value = decodeURIComponent(opts.name || '学习资料')
    } else if (opts?.url) {
      fileUrl.value = decodeURIComponent(opts.url)
      fileName.value = decodeURIComponent(opts.name || '文件')
      fileExt.value = opts.ext ? decodeURIComponent(opts.ext) : getFileExt(fileName.value)
      mode.value = detectMode(fileName.value, fileExt.value)
    }
    uni.setNavigationBarTitle({ title: (fileName.value || '预览').substring(0, 12) })
    if (mode.value === 'text' && fileUrl.value) {
      await loadTextContent(fileUrl.value)
    } else if (mode.value === 'pdf' && fileUrl.value && !pdfBuffer) {
      await showPdf(fileUrl.value)
    } else if ((mode.value === 'office' || mode.value === 'other') && fileUrl.value) {
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

onUnmounted(() => stopPoll())
</script>
<style scoped>
.preview-page { min-height: 100vh; background: var(--hai-bg); padding: 24rpx 32rpx 160rpx; box-sizing: border-box; }
.loading { text-align: center; padding: 80rpx; color: var(--hai-text-muted); }
.file-name { font-size: 28rpx; color: var(--hai-text-secondary); display: block; margin-bottom: 20rpx; word-break: break-all; }
.convert-hint { font-size: 24rpx; color: var(--hai-primary); display: block; margin-bottom: 16rpx; }
.preview-image { width: 100%; border-radius: 24rpx; background: var(--hai-card); box-shadow: var(--hai-shadow); }
.preview-video { width: 100%; height: 420rpx; border-radius: 24rpx; background: #000; }
.preview-audio { width: 100%; margin: 40rpx 0; }
.text-box { height: calc(100vh - 280rpx); background: var(--hai-card); border-radius: 24rpx; padding: 24rpx; box-sizing: border-box; box-shadow: var(--hai-shadow); }
.text-content { font-size: 26rpx; color: var(--hai-text); line-height: 1.7; white-space: pre-wrap; word-break: break-all; }
.pdf-box { background: var(--hai-card); border-radius: 24rpx; padding: 12rpx; box-shadow: var(--hai-shadow); }
.pdf-canvas { width: 100%; display: block; }
.pdf-nav { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 8rpx; }
.pdf-page { font-size: 24rpx; color: var(--hai-text-secondary); }
.doc-box { background: var(--hai-card); border-radius: 28rpx; padding: 80rpx 40rpx; text-align: center; box-shadow: var(--hai-shadow); }
.doc-icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
.doc-tip { font-size: 28rpx; color: var(--hai-text-secondary); display: block; margin-bottom: 40rpx; }
.action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: var(--hai-card); border-top: 1rpx solid var(--hai-border);
}
</style>
