<route lang="json5">{ style: { navigationBarTitleText: '文件预览', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="preview-page">
    <view v-if="loading" class="loading">加载中...</view>
    <HomeEmpty
      v-else-if="loadFailed"
      title="预览加载失败"
      hint="请检查网络后重试"
      action-text="重试"
      :card="true"
      @action="reloadPreview"
    />
    <template v-else>
      <text class="file-name">{{ fileName }}</text>
      <text v-if="hint" class="convert-hint">{{ hint }}</text>

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
        <wd-button v-if="pdfFailed" type="primary" @click="openDocument">用系统打开</wd-button>
      </view>

      <view v-else class="doc-box">
        <text class="doc-icon">📄</text>
        <text class="doc-tip">{{ mode === 'office' ? '将用手机上的应用打开（如 WPS、微信）' : '将用手机上的应用打开' }}</text>
        <wd-button type="primary" @click="openDocument">打开文件</wd-button>
      </view>

      <view class="action-bar">
        <wd-button v-if="mode === 'image'" type="primary" block :loading="acting" @click="handleSaveImage">
          保存到相册
        </wd-button>
        <wd-button v-else-if="mode === 'video'" type="primary" block :loading="acting" @click="handleDownload">
          保存视频到相册
        </wd-button>
        <wd-button v-else type="primary" block :loading="acting" @click="handleDownload">
          打开文件
        </wd-button>
      </view>
    </template>
  </view>
</template>
<script lang="ts" setup>
import { nextTick, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import NativeHtmlAudio from '../../pages-homeai/components/NativeHtmlAudio'
import { storageApi } from '../../pages-homeai/api/storage'
import { learnApi } from '../../pages-homeai/api/learn'
import { resolveContentUrl } from '../../pages-homeai/utils/contentUrl'
import { downloadStorageFile, saveStorageImage } from '../../pages-homeai/utils/fileDownload'
import { getStorageDisplayName, normalizeStorageFile } from '../../pages-homeai/utils/storageFileDisplay'
import { downloadToTemp, openLocalDocument } from '../../pages-homeai/platform/download'
import { isCapacitorNative } from '../../pages-homeai/platform/runtime'
import { fetchPdfBuffer, renderPdfPage } from '../../pages-homeai/utils/pdfPreview'
import HomeEmpty from '../../components/HomeEmpty.vue'
import { useHomeaiPageGuard } from '../../pages-homeai/utils/useHomeaiPageGuard'

useHomeaiPageGuard()
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
const loadFailed = ref(false)
const acting = ref(false)
let lastOpts: any = null
const fileId = ref('')
const materialId = ref('')
const fileUrl = ref('')
const fileName = ref('')
const fileExt = ref('')
const mode = ref<PreviewMode>('other')
const textContent = ref('')
const tempFilePath = ref('')
const hint = ref('')
const pdfPage = ref(1)
const pdfTotal = ref(1)
const pdfFailed = ref(false)
let pdfBuffer: ArrayBuffer | null = null

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
    materialId: materialId.value || undefined,
    fileUrl: fileUrl.value,
    originalName: fileName.value,
    extension: fileExt.value,
  }
}

/** 文档/PDF/文本走后端鉴权流，避免 OSS 预签名在 WebView 里被 CORS 拦 */
function contentOrFileUrl(): string {
  return resolveContentUrl({
    id: fileId.value || undefined,
    materialId: materialId.value || undefined,
    fileUrl: fileUrl.value,
  })
}

async function loadTextContent(url: string) {
  const temp = await downloadToTemp(url, fileName.value || 'text.txt')
  tempFilePath.value = temp
  if (isCapacitorNative() && !/^https?:\/\//i.test(temp)) {
    const { capacitorReadBase64, base64ToUtf8 } = await import('../../pages-homeai/platform/capDownload')
    textContent.value = base64ToUtf8(await capacitorReadBase64(temp))
    return
  }
  if (typeof fetch === 'function' && /^https?:\/\//i.test(temp)) {
    const res = await fetch(temp)
    textContent.value = await res.text()
    return
  }
  const fsm: any = uni.getFileSystemManager?.()
  if (!fsm) {
    textContent.value = '当前平台不支持文本预览，请下载查看'
    return
  }
  await new Promise<void>((resolve, reject) => {
    fsm.readFile({
      filePath: temp,
      encoding: 'utf-8',
      success: (r: any) => {
        textContent.value = typeof r.data === 'string' ? r.data : String(r.data)
        resolve()
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
    hint.value = '页内预览失败，可尝试用系统打开'
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

function applyPreviewMeta(data?: any) {
  if (data?.fileName) fileName.value = data.fileName
  if (data?.extension) fileExt.value = data.extension
  const next = detectMode(fileName.value, fileExt.value, data?.kind)
  // 图片展示沿用列表/详情的压缩图；文档必须用原文件，不再转 PDF
  if (data?.fileUrl && (next !== 'image' || !fileUrl.value)) {
    fileUrl.value = data.fileUrl
  }
  mode.value = next
}

function previewFullImage() {
  if (fileUrl.value) previewImageUrl(fileUrl.value)
}

function openDocument() {
  if (tempFilePath.value && !/^https?:\/\//i.test(tempFilePath.value)) {
    openLocalDocument(tempFilePath.value, fileName.value)
    return
  }
  handleDownload()
}

async function handleSaveImage() {
  if (acting.value) return
  acting.value = true
  try {
    await saveStorageImage(fileInput())
  } finally {
    acting.value = false
  }
}

async function handleDownload() {
  if (acting.value) return
  acting.value = true
  try {
    const path = await downloadStorageFile(fileInput())
    if (path) tempFilePath.value = path
  } finally {
    acting.value = false
  }
}

async function loadPreview(opts: any) {
  lastOpts = opts
  loading.value = true
  loadFailed.value = false
  try {
    if (opts?.fileId) {
      fileId.value = opts.fileId
      const file = normalizeStorageFile(await storageApi.fileDetail(opts.fileId))
      fileUrl.value = file.fileUrl || ''
      fileName.value = getStorageDisplayName(file)
      fileExt.value = file.extension || getFileExt(fileName.value)
      applyPreviewMeta(await storageApi.preview(fileId.value))
    } else if (opts?.materialId) {
      materialId.value = opts.materialId
      applyPreviewMeta(await learnApi.preview(materialId.value))
      if (!fileName.value) fileName.value = decodeURIComponent(opts.name || '学习资料')
    } else if (opts?.url) {
      fileUrl.value = decodeURIComponent(opts.url)
      fileName.value = decodeURIComponent(opts.name || '文件')
      fileExt.value = opts.ext ? decodeURIComponent(opts.ext) : getFileExt(fileName.value)
      mode.value = detectMode(fileName.value, fileExt.value)
    }
    uni.setNavigationBarTitle({ title: (fileName.value || '预览').substring(0, 12) })
    const sourceUrl = contentOrFileUrl()
    if (mode.value === 'text' && sourceUrl) {
      await loadTextContent(sourceUrl)
    } else if (mode.value === 'pdf' && sourceUrl && !pdfBuffer) {
      await showPdf(sourceUrl)
    }
  } catch (e: any) {
    loadFailed.value = true
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function reloadPreview() {
  if (lastOpts) loadPreview(lastOpts)
}

onLoad((opts: any) => {
  loadPreview(opts || {})
})
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
