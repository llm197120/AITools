<template>
  <view class="home-media-upload">
    <!-- 已上传内容预览 -->
    <view v-if="modelValue && !uploading" class="hmu-preview">
      <!-- 图片预览 -->
      <view v-if="mode === 'image'" class="hmu-img-wrap">
        <image
          class="hmu-img"
          :style="{ height: props.height + 'rpx' }"
          :src="modelValue"
          mode="aspectFill"
        />
        <view class="hmu-mask">
          <view class="hmu-mask-item" @click.stop="pickAndUpload">更换</view>
          <view class="hmu-mask-item danger" @click.stop="clearValue">删除</view>
        </view>
      </view>
      <!-- 视频预览 -->
      <view v-else-if="mode === 'video'" class="hmu-video-wrap">
        <video class="hmu-video" :src="modelValue" controls></video>
        <view class="hmu-actions">
          <view class="hmu-btn" @click="pickAndUpload">更换</view>
          <view class="hmu-btn danger" @click="clearValue">删除视频</view>
        </view>
      </view>
      <!-- 音频预览 -->
      <view v-else-if="mode === 'audio'" class="hmu-audio-wrap">
        <NativeHtmlAudio :src="modelValue" />
        <view class="hmu-actions">
          <view class="hmu-btn" @click="pickAndUpload">更换</view>
          <view class="hmu-btn danger" @click="clearValue">删除音频</view>
        </view>
      </view>
      <!-- 文件预览 -->
      <view v-else class="hmu-file">
        <view class="hmu-file-icon">📄</view>
        <view class="hmu-file-info">
          <text class="hmu-file-name">{{ fileName }}</text>
          <text class="hmu-file-link" @click="openFile">查看</text>
        </view>
        <view class="hmu-file-ops">
          <view class="hmu-btn" @click="pickAndUpload">更换</view>
          <view class="hmu-btn danger" @click="clearValue">删除</view>
        </view>
      </view>
    </view>

    <!-- 上传中：进度反馈 -->
    <view v-else-if="uploading" class="hmu-empty" :style="{ minHeight: props.height + 'rpx' }">
      <view class="hmu-empty-icon">⏳</view>
      <text class="hmu-empty-text">上传中 {{ progress }}%</text>
      <view class="hmu-progress">
        <view class="hmu-progress-bar" :style="{ width: progress + '%' }"></view>
      </view>
    </view>

    <!-- 空状态：点击选择 -->
    <view
      v-else
      class="hmu-empty"
      :style="{ minHeight: props.height + 'rpx' }"
      @click="pickAndUpload"
    >
      <view class="hmu-empty-icon">{{ emptyIcon }}</view>
      <text class="hmu-empty-text">{{ placeholder }}</text>
      <text v-if="tip" class="hmu-tip">{{ tip }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { getServerBaseUrl } from '../api/request'
import { getToken } from '../utils/auth'
import { consumeHomeaiUnauthorized } from '../utils/homeaiAuth'
import { AUDIO_EXTS } from '../platform/fileAccept'
import { useHomeaiFilePick } from '../utils/useHomeaiFilePick'
import { previewFile } from '../utils/filePreview'
import NativeHtmlAudio from './NativeHtmlAudio'

const props = defineProps({
  /** 当前媒体地址 */
  modelValue: { type: String, default: '' },
  /** 模式：image / video / audio / file（学习 add 用 file/audio） */
  mode: { type: String, default: 'file' },
  /** 上传接口完整路径，如 /homeai/recipe/cover */
  url: { type: String, required: true },
  /** 附加表单参数（如 learn 的 type） */
  formData: { type: Object, default: () => ({}) },
  /** 空状态文案 */
  placeholder: { type: String, default: '' },
  /** 辅助提示文案 */
  tip: { type: String, default: '' },
  /** 最大文件大小（MB） */
  maxSize: { type: Number, default: 50 },
  /** 视频最长时长（秒） */
  maxVideoDuration: { type: Number, default: 60 },
  /** 图片/空状态高度（rpx） */
  height: { type: Number, default: 320 },
  /** 额外收窄扩展名（如学习资料按 type） */
  allowedExt: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'change'])

const { pickImages, pickVideo, pickFiles } = useHomeaiFilePick()

const uploading = ref(false)
const progress = ref(0)
const fileName = ref('')

const emptyIcon = computed(() => {
  if (props.mode === 'image') return '🖼️'
  if (props.mode === 'video') return '🎬'
  if (props.mode === 'audio') return '🎵'
  return '📄'
})

async function pickAndUpload() {
  if (uploading.value) return
  let files: any[] = []
  const extra = (props.allowedExt || []) as string[]
  if (props.mode === 'image' && extra.length === 0) {
    files = await pickImages({ count: 1 })
  } else if (props.mode === 'video' && extra.length === 0) {
    files = await pickVideo({ maxDuration: props.maxVideoDuration })
  } else if (props.mode === 'audio' || extra.length) {
    const exts = extra.length ? extra : [...AUDIO_EXTS]
    files = await pickFiles({
      count: 1,
      type: props.mode === 'video' ? 'video' : props.mode === 'image' ? 'image' : props.mode === 'audio' ? 'all' : 'file',
      extension: exts,
      allowedExt: exts,
    })
  } else {
    files = await pickFiles({ count: 1, type: 'all' })
  }
  if (!files[0]) return
  if (props.maxSize && files[0].size && files[0].size > props.maxSize * 1024 * 1024) {
    uni.showToast({ title: `文件不能超过 ${props.maxSize}MB`, icon: 'none' })
    return
  }
  fileName.value = files[0].name || ''
  uploading.value = true
  progress.value = 0
  try {
    const url = await uploadFile(files[0].path)
    emit('change', url)
    emit('update:modelValue', url)
    uni.showToast({ title: '上传成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '上传失败', icon: 'none' })
  } finally {
    uploading.value = false
  }
}

function uploadFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: getServerBaseUrl() + props.url,
      filePath,
      name: 'file',
      formData: props.formData,
      header: { 'X-Access-Token': getToken() || '' },
      timeout: 120000,
      onProgressUpdate: (res) => {
        if (res.progress) progress.value = res.progress
      },
      success: (res) => {
        try {
          if (consumeHomeaiUnauthorized(res.statusCode, res.data)) {
            reject(new Error('登录已过期'))
            return
          }
          const data = JSON.parse(res.data)
          if (data.success && data.result) resolve(data.result)
          else reject(new Error(data.message || '上传失败'))
        } catch (e) {
          reject(e)
        }
      },
      fail: reject,
    })
  })
}

function openFile() {
  previewFile({ fileUrl: props.modelValue, originalName: fileName.value })
}

function clearValue() {
  emit('change', '')
  emit('update:modelValue', '')
}
</script>

<style scoped>
.home-media-upload {
  width: 100%;
}
.hmu-preview {
  margin-bottom: 16rpx;
}
.hmu-img-wrap {
  position: relative;
  border-radius: var(--hai-radius-md, 24rpx);
  overflow: hidden;
}
.hmu-img {
  width: 100%;
  height: 320rpx;
  display: block;
}
.hmu-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: rgba(0, 0, 0, 0.45);
  padding: 12rpx 0;
}
.hmu-mask-item {
  flex: 1;
  text-align: center;
  color: #fff;
  font-size: 26rpx;
}
.hmu-mask-item.danger {
  color: #ff7875;
}
.hmu-video-wrap .hmu-video {
  width: 100%;
  height: 360rpx;
  border-radius: var(--hai-radius-md, 24rpx);
}
.hmu-audio-wrap .hmu-audio {
  width: 100%;
  margin: 12rpx 0;
}
.hmu-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 12rpx;
}
.hmu-file {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: var(--hai-bg, #f3f2ee);
  border-radius: var(--hai-radius-md, 24rpx);
}
.hmu-file-icon {
  font-size: 44rpx;
}
.hmu-file-info {
  flex: 1;
  min-width: 0;
}
.hmu-file-name {
  display: block;
  font-size: 28rpx;
  color: var(--hai-text, #2c271f);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hmu-file-link {
  display: inline-block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: var(--hai-primary, #1677ff);
}
.hmu-file-ops {
  display: flex;
  gap: 16rpx;
}
.hmu-btn {
  font-size: 26rpx;
  color: var(--hai-primary, #1677ff);
  padding: 8rpx 16rpx;
  background: var(--hai-card, #fff);
  border-radius: 12rpx;
}
.hmu-btn.danger {
  color: var(--hai-danger, #e54d42);
}
.hmu-empty {
  border: 2rpx dashed var(--hai-border, #d9d9d9);
  border-radius: var(--hai-radius-md, 24rpx);
  background: var(--hai-bg, #f3f2ee);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}
.hmu-empty-icon {
  font-size: 56rpx;
}
.hmu-empty-text {
  font-size: 26rpx;
  color: var(--hai-text-muted, #999);
}
.hmu-tip {
  font-size: 22rpx;
  color: var(--hai-text-muted, #999);
}
.hmu-progress {
  width: 60%;
  height: 12rpx;
  background: rgba(0, 0, 0, 0.08);
  border-radius: 999rpx;
  overflow: hidden;
}
.hmu-progress-bar {
  height: 100%;
  background: var(--hai-primary, #1677ff);
  border-radius: 999rpx;
  transition: width 0.2s;
}
</style>
