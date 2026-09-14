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
        @click="previewFullImage"
        @longpress="showExternalSheet"
      />

      <video
        v-else-if="mode === 'video'"
        class="preview-video"
        :src="fileUrl"
        controls
        @longpress="showExternalSheet"
      />

      <NativeHtmlAudio v-else-if="mode === 'audio'" class="preview-audio" :src="fileUrl" />

      <scroll-view v-else-if="mode === 'text'" scroll-y class="text-box">
        <text class="text-content" selectable>{{ textContent }}</text>
      </scroll-view>

      <view v-else-if="mode === 'pdf'" class="pdf-box" @longpress="showExternalSheet">
        <canvas id="homeai-pdf-canvas" class="pdf-canvas" />
        <view v-if="pdfTotal > 1" class="pdf-nav">
          <wd-button size="small" :disabled="pdfPage <= 1" @click="changePdfPage(-1)">上一页</wd-button>
          <text class="pdf-page">{{ pdfPage }} / {{ pdfTotal }}</text>
          <wd-button size="small" :disabled="pdfPage >= pdfTotal" @click="changePdfPage(1)">下一页</wd-button>
        </view>
        <wd-button v-if="pdfFailed" type="primary" :loading="acting" @click="handleOpenExternal">用其他应用打开</wd-button>
      </view>

      <view v-else-if="officeLoading" class="doc-box" @longpress="showExternalSheet">
        <text class="doc-icon">⏳</text>
        <text class="doc-tip">正在打开文档…</text>
        <wd-button type="primary" :loading="acting" @click="handleOpenExternal">用其他应用打开</wd-button>
      </view>

      <view v-else-if="mode === 'office' && !officeFailed" class="office-box" @longpress="showExternalSheet">
        <scroll-view v-if="excelSheets.length > 1" scroll-x class="sheet-tabs">
          <text
            v-for="(name, i) in excelSheets"
            :key="name + i"
            class="sheet-tab"
            :class="{ active: i === excelSheetIndex }"
            @click="changeExcelSheet(i)"
          >{{ name }}</text>
        </scroll-view>
        <view class="office-scroll">
          <div id="homeai-office-host" class="office-host"></div>
        </view>
        <view v-if="pptTotal > 1" class="pdf-nav">
          <wd-button size="small" :disabled="pptPage <= 1" @click="changePptPage(-1)">上一页</wd-button>
          <text class="pdf-page">{{ pptPage }} / {{ pptTotal }}</text>
          <wd-button size="small" :disabled="pptPage >= pptTotal" @click="changePptPage(1)">下一页</wd-button>
        </view>
      </view>

      <view v-else class="doc-box" @longpress="showExternalSheet">
        <text class="doc-icon">📄</text>
        <text class="doc-tip">{{ officeHint }}</text>
        <wd-button type="primary" :loading="acting" @click="handleOpenExternal">用其他应用打开</wd-button>
      </view>

      <view class="action-bar" :class="{ 'action-bar--split': mode === 'image' || mode === 'video' }">
        <wd-button v-if="mode === 'image'" type="primary" block :loading="acting" @click="handleSaveImage">
          保存到相册
        </wd-button>
        <wd-button v-else-if="mode === 'video'" type="primary" block :loading="acting" @click="handleDownload">
          保存视频到相册
        </wd-button>
        <wd-button type="primary" block :loading="acting" @click="handleOpenExternal">
          用其他应用打开
        </wd-button>
      </view>
    </template>
    <view v-if="imageZoomed && fileUrl" class="image-zoom" @click="closeImageZoom" @touchmove.stop.prevent>
      <image class="image-zoom-img" :src="fileUrl" mode="aspectFit" @click.stop="closeImageZoom" />
    </view>
    <wd-action-sheet
      v-model="externalSheetVisible"
      :actions="externalSheetActions"
      cancel-text="取消"
      @select="onExternalSheetSelect"
    />
  </view>
</template>
<script lang="ts" setup>
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { onBackPress, onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import NativeHtmlAudio from '../../pages-homeai/components/NativeHtmlAudio'
import { storageApi } from '../../pages-homeai/api/storage'
import { learnApi } from '../../pages-homeai/api/learn'
import { FILE_OPEN_EXTERNALLY_NAME, resolveContentUrl } from '../../pages-homeai/utils/contentUrl'
import { downloadStorageFile, openStorageFileExternally, saveStorageImage } from '../../pages-homeai/utils/fileDownload'
import { getStorageDisplayName, normalizeStorageFile } from '../../pages-homeai/utils/storageFileDisplay'
import { downloadToTemp, openLocalDocument } from '../../pages-homeai/platform/download'
import { isCapacitorNative, registerHardwareBackHandler } from '../../pages-homeai/platform/runtime'
import { fetchPdfBuffer, fetchPreviewBuffer, renderPdfPage } from '../../pages-homeai/utils/pdfPreview'
import {
  officeNeedsExternalApp,
  prepareOfficePreview,
  type OfficePreviewHandle,
} from '../../pages-homeai/utils/officeNativePreview'
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
const officeLoading = ref(false)
const officeFailed = ref(false)
const excelSheets = ref<string[]>([])
const excelSheetIndex = ref(0)
const pptPage = ref(1)
const pptTotal = ref(1)
const pdfPage = ref(1)
const pdfTotal = ref(1)
const pdfFailed = ref(false)
const imageZoomed = ref(false)
const externalSheetVisible = ref(false)
let previewAlive = true
let pdfBuffer: ArrayBuffer | null = null
let officeHandle: OfficePreviewHandle | null = null

const officeHint = computed(() => {
  if (mode.value === 'office') {
    return officeFailed.value
      ? (hint.value || '暂无法页内预览，可用 WPS、微信等应用打开原文件')
      : '正在准备预览'
  }
  return '可用手机上的其他应用打开此文件'
})
const externalSheetActions = computed(() => {
  const actions = [{ name: FILE_OPEN_EXTERNALLY_NAME, key: 'openExternal' }]
  if (mode.value === 'image') actions.unshift({ name: '保存到相册', key: 'saveAlbum' })
  return actions
})

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
    hint.value = '页内预览失败，可用其他应用打开'
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

async function paintOffice() {
  await nextTick()
  const host = document.getElementById('homeai-office-host') as HTMLElement | null
  if (!host || !officeHandle) {
    officeFailed.value = true
    hint.value = '无法显示文档预览'
    return
  }
  await officeHandle.render(host, {
    sheetIndex: excelSheetIndex.value,
    pptPage: pptPage.value,
  })
}

async function showOffice(url: string) {
  officeFailed.value = false
  officeLoading.value = true
  excelSheets.value = []
  excelSheetIndex.value = 0
  pptPage.value = 1
  pptTotal.value = 1
  officeHandle = null
  try {
    const ext = (fileExt.value || getFileExt(fileName.value)).replace(/^\./, '').toLowerCase()
    if (officeNeedsExternalApp(ext)) {
      throw new Error('旧版 .doc / .ppt 无法在 App 内预览，请用其他应用打开')
    }
    const data = await fetchPreviewBuffer(url, fileName.value || `preview.${ext || 'bin'}`)
    officeHandle = await prepareOfficePreview(ext, data)
    excelSheets.value = officeHandle.sheets
    pptTotal.value = officeHandle.pptTotal
    if (!previewAlive) return
    hint.value = '页内预览供查阅，复杂排版可用其他应用打开'
    await paintOffice()
  } catch (e: any) {
    if (!previewAlive) return
    officeFailed.value = true
    hint.value = e?.message || '文档预览失败'
  } finally {
    if (previewAlive) officeLoading.value = false
  }
}

async function changeExcelSheet(index: number) {
  excelSheetIndex.value = index
  await paintOffice()
}

async function changePptPage(delta: number) {
  const next = pptPage.value + delta
  if (next < 1 || next > pptTotal.value) return
  pptPage.value = next
  await paintOffice()
}

function applyPreviewMeta(data?: any) {
  if (data?.fileName) fileName.value = data.fileName
  if (data?.extension) fileExt.value = data.extension
  const next = detectMode(fileName.value, fileExt.value, data?.kind)
  if (data?.fileUrl && (next !== 'image' || !fileUrl.value)) {
    fileUrl.value = data.fileUrl
  }
  mode.value = next
}

function closeImageZoom() {
  imageZoomed.value = false
}

function previewFullImage() {
  if (fileUrl.value) imageZoomed.value = true
}

function consumePreviewBack(): boolean {
  try {
    uni.hideLoading()
  } catch {
    // ignore
  }
  if (imageZoomed.value) {
    closeImageZoom()
    return true
  }
  return false
}

function showExternalSheet() {
  externalSheetVisible.value = true
}

function onExternalSheetSelect({ index }: { index: number }) {
  const action = externalSheetActions.value[index]
  if (!action) return
  if (action.key === 'saveAlbum') handleSaveImage()
  else handleOpenExternal()
}

async function handleOpenExternal() {
  if (acting.value) return
  acting.value = true
  try {
    if (tempFilePath.value && !/^https?:\/\//i.test(tempFilePath.value)) {
      await openLocalDocument(tempFilePath.value, fileName.value)
      return
    }
    const path = await openStorageFileExternally(fileInput())
    if (path) tempFilePath.value = path
  } finally {
    acting.value = false
  }
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
  officeLoading.value = false
  officeFailed.value = false
  excelSheets.value = []
  excelSheetIndex.value = 0
  pptPage.value = 1
  pptTotal.value = 1
  officeHandle = null
  pdfBuffer = null
  pdfPage.value = 1
  pdfTotal.value = 1
  pdfFailed.value = false
  hint.value = ''
  tempFilePath.value = ''
  fileId.value = ''
  materialId.value = ''
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
    if (mode.value === 'office') officeLoading.value = true
    loading.value = false
    const sourceUrl = contentOrFileUrl()
    if (mode.value === 'text' && sourceUrl) {
      await loadTextContent(sourceUrl)
    } else if (mode.value === 'pdf' && sourceUrl) {
      hint.value = '正在加载 PDF…'
      await showPdf(sourceUrl)
      if (!pdfFailed.value) hint.value = ''
    } else if (mode.value === 'office') {
      if (!sourceUrl) {
        officeLoading.value = false
        officeFailed.value = true
        hint.value = '无法获取文件地址'
        return
      }
      await showOffice(sourceUrl)
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
  previewAlive = true
  loadPreview(opts || {})
})

onHide(() => {
  uni.hideLoading()
})

onUnload(() => {
  previewAlive = false
  imageZoomed.value = false
  uni.hideLoading()
})

onBackPress(() => {
  // Capacitor 壳由 App.backButton 统一处理，这里只挡住 uni 再 pop 一次
  if (isCapacitorNative()) return true
  return consumePreviewBack()
})

const unregisterBack = registerHardwareBackHandler(consumePreviewBack)
onUnmounted(() => unregisterBack())
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
.office-box { background: var(--hai-card); border-radius: 24rpx; padding: 12rpx; box-shadow: var(--hai-shadow); }
.sheet-tabs { white-space: nowrap; margin-bottom: 8rpx; }
.sheet-tab {
  display: inline-block;
  margin-right: 12rpx;
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  background: var(--hai-bg);
  border-radius: 12rpx;
}
.sheet-tab.active { color: var(--hai-primary); font-weight: 600; }
.office-scroll {
  overflow: auto;
  max-height: calc(100vh - 360rpx);
  -webkit-overflow-scrolling: touch;
}
.office-host { min-height: 200rpx; font-size: 28rpx; color: var(--hai-text); line-height: 1.6; }
.office-host :deep(.homeai-docx-wrapper),
.office-host :deep(.docx-wrapper) { background: #fff; }
.office-host :deep(p) { margin: 0 0 16rpx; }
.office-host :deep(img) { max-width: 100%; height: auto; display: block; margin: 12rpx 0; }
.office-host :deep(table) { border-collapse: collapse; font-size: 22rpx; }
.office-host :deep(td),
.office-host :deep(th) { border: 1px solid var(--hai-border); padding: 8rpx 12rpx; white-space: nowrap; }
.office-host :deep(.ppt-slide) { min-height: 320rpx; padding: 12rpx; }
.action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: var(--hai-card); border-top: 1rpx solid var(--hai-border);
}
.action-bar--split {
  display: flex;
  gap: 16rpx;
}
.action-bar--split :deep(.wd-button) {
  flex: 1;
}
.image-zoom {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-zoom-img {
  width: 100%;
  height: 100%;
}
</style>
