<route lang="json5">{ style: { navigationBarTitleText: '学习资料', navigationBarBackgroundColor: '#F3F2EE' } }</route>
<template>
  <view class="hai-page" :class="{ 'hai-page--timer': learning }">
    <text class="title">{{ material.title || title }}</text>
    <text class="meta" v-if="material.category">{{ material.category }} · {{ material.type }}</text>

    <!-- 视频预览 -->
    <video v-if="isVideo" class="media" :src="mediaUrl" controls></video>
    <image v-else-if="isImage" class="media image" :src="mediaUrl" mode="widthFix" @click="previewImage" />
    <NativeHtmlAudio v-else-if="isAudio" class="media-audio" :src="mediaUrl" />
    <!-- 链接 -->
    <view v-else-if="isLink && linkUrl" class="link-box">
      <text class="link-url">{{ linkUrl }}</text>
      <wd-button size="small" @click="openLink">{{ linkActionLabel }}</wd-button>
    </view>
    <!-- 其他文档提示 -->
    <view v-else-if="mediaUrl" class="doc-tip" @click="openDocument">
      <text class="doc-icon">📄</text>
      <text>点击打开文档</text>
    </view>

    <view v-if="material.description" class="desc">{{ material.description }}</view>

    <view v-if="learning" class="timer-bar">
      <text>学习中 {{ formatElapsed(elapsed) }}</text>
      <wd-button size="small" type="warning" @click="stopLearn">结束学习</wd-button>
    </view>
    <view v-else class="actions">
      <wd-button size="large" type="primary" block @click="startLearn">开始学习</wd-button>
      <wd-button size="large" plain block @click="goEdit">编辑</wd-button>
      <wd-button size="large" type="error" plain block @click="deleteVisible = true">删除</wd-button>
    </view>
  </view>

  <wd-popup v-model="deleteVisible" position="center" custom-style="width:80%;border-radius:28rpx;overflow:hidden">
    <view class="dialog-title">删除资料</view>
    <view class="dialog-body">
      <text class="dialog-hint">确定删除该学习资料？删除后无法恢复。</text>
    </view>
    <view class="dialog-footer">
      <wd-button block @click="deleteVisible = false">取消</wd-button>
      <wd-button type="error" block @click="doDelete">确认删除</wd-button>
    </view>
  </wd-popup>
</template>
<script lang="ts" setup>
import { computed, ref, onUnmounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import NativeHtmlAudio from '../../pages-homeai/components/NativeHtmlAudio'
import { learnApi } from '../../pages-homeai/api/learn'
import { getFileExt, isAudioExt, isImageExt, isVideoExt, previewFile } from '../../pages-homeai/utils/filePreview'
import { isStandaloneApp, openExternalUrl } from '../../pages-homeai/platform/runtime'

const materialId = ref('')
const material = ref<any>({})
const title = ref('')
const mediaUrl = ref('')
const linkUrl = ref('')
const learning = ref(false)
const elapsed = ref(0)
/** 列表跳转带 autoStart=1 时，无进行中会话则自动开始学习 */
const autoStart = ref(false)
const deleteVisible = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const isVideo = computed(() => material.value.type === 'video' || isVideoExt(getFileExt(mediaUrl.value)))
const isImage = computed(() => material.value.type === 'image' || isImageExt(getFileExt(mediaUrl.value)))
const isAudio = computed(() => material.value.type === 'audio' || isAudioExt(getFileExt(mediaUrl.value)))
const isLink = computed(() => material.value.type === 'link')
const linkActionLabel = computed(() => (isStandaloneApp() ? '在浏览器中打开' : '复制链接'))

function formatElapsed(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function startTimer(base = 0) {
  clearTimer()
  elapsed.value = base
  timer = setInterval(() => {
    elapsed.value += 1
  }, 1000)
}

async function loadMaterial(id: string) {
  const m = await learnApi.materialById(id)
  if (!m) {
    uni.showToast({ title: '资料不存在', icon: 'none' })
    return false
  }
  material.value = m
  mediaUrl.value = m.fileUrl || ''
  if (m.type === 'link') linkUrl.value = m.fileUrl || ''
  uni.setNavigationBarTitle({ title: m.title || '学习资料' })
  return true
}

async function syncSession() {
  const session: any = await learnApi.activeSession()
  if (session?.materialId === materialId.value) {
    learning.value = true
    startTimer(session.elapsedSeconds || 0)
  }
}

function previewImage() {
  if (mediaUrl.value) uni.previewImage({ urls: [mediaUrl.value] })
}

function openDocument() {
  previewFile({
    materialId: materialId.value,
    fileUrl: mediaUrl.value,
    originalName: material.value.title,
    title: material.value.title,
  })
}

function openLink() {
  if (!linkUrl.value) return
  if (isStandaloneApp()) {
    openExternalUrl(linkUrl.value)
    return
  }
  uni.setClipboardData({
    data: linkUrl.value,
    success: () => uni.showToast({ title: '链接已复制', icon: 'success' }),
  })
}

async function startLearn() {
  await learnApi.start(materialId.value)
  learning.value = true
  startTimer(0)
  uni.showToast({ title: '已开始学习', icon: 'success' })
}

async function stopLearn() {
  await learnApi.stop(materialId.value)
  learning.value = false
  clearTimer()
  uni.showToast({ title: '已记录学习时长', icon: 'success' })
}

function goEdit() {
  if (!materialId.value) return
  uni.navigateTo({ url: `/pages-homeai-more/learn/add?id=${materialId.value}` })
}

async function doDelete() {
  if (!materialId.value) return
  try {
    await learnApi.remove(materialId.value)
    deleteVisible.value = false
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    // request 层已 toast
  }
}

onLoad(async (opts: any) => {
  if (opts?.videoUrl) {
    mediaUrl.value = decodeURIComponent(opts.videoUrl)
    title.value = decodeURIComponent(opts.title || '视频')
    uni.setNavigationBarTitle({ title: title.value })
    return
  }
  materialId.value = opts?.id || ''
  autoStart.value = opts?.autoStart === '1' || opts?.autoStart === 1 || opts?.autoStart === true
  if (!materialId.value) {
    uni.showToast({ title: '参数错误', icon: 'none' })
    return
  }
  const loaded = await loadMaterial(materialId.value)
  await syncSession()
  // 仅资料加载成功且无进行中会话时，按 autoStart 自动开始计时
  if (loaded && autoStart.value && !learning.value) {
    await startLearn()
  }
})

onShow(async () => {
  if (!materialId.value) return
  await loadMaterial(materialId.value)
})

onUnmounted(() => clearTimer())
</script>
<style scoped>
/* page shell: .hai-page */
.title { font-family: var(--hai-serif); font-size: 36rpx; font-weight: 700; color: var(--hai-text); display: block; margin-bottom: 8rpx; }
.meta { font-size: 24rpx; color: var(--hai-text-muted); display: block; margin-bottom: 24rpx; }
.media { width: 100%; border-radius: 24rpx; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.media.image { background: var(--hai-card); }
.media-audio { width: 100%; margin: 16rpx 0 24rpx; }
.doc-tip { background: var(--hai-card); border-radius: 24rpx; padding: 60rpx; text-align: center; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); color: var(--hai-text); }
.link-box { background: var(--hai-card); border-radius: 24rpx; padding: 24rpx; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.link-url { font-size: 24rpx; color: var(--hai-primary); word-break: break-all; display: block; margin-bottom: 16rpx; }
.doc-icon { font-size: 64rpx; display: block; margin-bottom: 16rpx; }
.desc { background: var(--hai-card); border-radius: 24rpx; padding: 24rpx; font-size: 28rpx; color: var(--hai-text-secondary); line-height: 1.6; margin-bottom: 24rpx; box-shadow: var(--hai-shadow); }
.timer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: space-between; background: var(--hai-success); color: var(--hai-on-primary); padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom)); font-size: 28rpx; }
.actions { margin-top: 40rpx; display: flex; flex-direction: column; gap: 16rpx; }
.hai-page--timer { padding-bottom: calc(180rpx + env(safe-area-inset-bottom)); }
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
